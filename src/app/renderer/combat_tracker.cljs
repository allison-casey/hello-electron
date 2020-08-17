(ns app.renderer.combat-tracker
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [re-com.core :refer [horizontal-tabs single-dropdown input-text]]
            [app.renderer.subs :as subs]
            [app.renderer.events :as events]
            [goog.string :as gstring]
            [goog.string.format]
            [com.rpl.specter :as sp]
            [cuerdas.core :as cuerdas]
            [app.renderer.macros :refer [when-let*]]))

;; * Combat Tracker
;; ** Utility Elements
(defn kv [key value]
  [:p.m-0 [:strong key] " : " value])

(defn bar [] [:span.text-muted \u007C])

(defn to-clipboard [text]
  (.writeText js/navigator.clipboard text))

(defn pill [number badge-color]
  [:span.badge.badge-pill
   {:class [badge-color]}
   number])

(defn render-additional-markup
  [markup]
  (when markup
    [:ul.list-unstyled
     (for [{:keys [key value]} markup]
       ^{:key key}
       [:li [kv key value]])]))

(defn icon
  [fa-class & {:keys [cursor on-click style]}]
  [:div
   [:i.fa.fa-fw
    {:style    (merge style {:cursor cursor})
     :on-click on-click
     :class    [fa-class]}]])

(defn accordion
  [title & {:keys [children left right]
            :or {left [] right []}}]
  (let [s (r/atom {:open? false, :child-height 0})]
    (fn
      [title & {:keys [children left right]
                :or {left [] right []}}]
      (let [{:keys [open? child-height]} @s]
        [:div.accordion
         [:div.card
          ;; Accordion Header
          [:div.card-header
           [:div.d-flex.w-100
            ;; left side elements
            (when (seq left) (into [:div.d-flex.pad-children-left] left))

            ;; title elements
            [:div.flex-grow-1.ml-1.align-middle.mb-0 title]

            ;; right side elements
            (when (seq right) (into [:div] right))

            ;; drawer toggle icon
            [:a.ml-1.align-middle
             {:on-click #(swap! s update :open? not)
              :style {:cursor "pointer"}}
             [icon (if open? "fa-minus" "fa-plus")]]]]

          ;; Accordion Body
          [:div.collapse.show
           {:style {:max-height (if open? child-height 0)
                    :transition "max-height 0.8s"
                    :overflow   "hidden"}}
           ;; Capture the node ref to get the child height dynamically
           (into [:div.card-body
                  {:ref (fn [e] (when e (swap!
                                         s
                                         assoc
                                         :child-height
                                         (.-clientHeight e))))}]
                 children)]]]))))

;; ** Character Selector
(defn character-selector []
  (let [characters @(rf/subscribe [::subs/characters])
        active-character @(rf/subscribe [::subs/selected-character-id])]
    [:<>
     [:div.row [:div.col [:h4.text-center "Characters"]]]
     [:div.row
      [:div.col
       [:ul.list-group
        (for [{:keys [uuid name]} characters]
          ^{:key uuid}
          [:li.list-group-item
           {:on-click #(rf/dispatch [:set-selected-character-id uuid])
            :class ["list-group-item" (if (= uuid active-character) "active")]}
           name])]]]]))

;; ** Character Info Card
(defn ^:private ability-li
  [char {:keys [id name description cooldown back-in
                additional-markup duration-left ap] :as ability}]
  (let [on-cooldown? (pos? (or back-in 0))
        enough-ap?   (>= (or (:ap-left char) (:ap char)) ap)
        disabled?    (or on-cooldown? (not enough-ap?))]
    [:div.list-group-item.list-group-item-action.flex-column.align-items-start
     {:on-mouse-enter #(rf/dispatch [:set-highlighted-ability (:uuid char) id])
      :on-mouse-leave #(rf/dispatch [:remove-highlighted-ability])
      :style          {:padding 0, :margin-bottom "0.25em"}}
     [accordion
      name
      :left [;; Use ability button
             [icon
              (if disabled? "fa-times-circle-o" "fa-check-circle-o")
              :style     {:cursor (if disabled? "not-allowed" "pointer")}
              :on-click  (fn [event]
                           (when-not disabled?
                             (rf/dispatch
                              [:use-ability (:uuid char) ability]))
                           (.preventDefault event))]
             [bar]
             ;; Copy accuracy button
             [icon
              "fa-crosshairs"
              :cursor   "pointer"
              :on-click #(to-clipboard "/roll 1d20 + 4")]
             [icon "fa-bomb" :cursor "pointer"] ;; Copy damage button
             [bar]
             ;; AP Cost
             [:div.text-muted (gstring/format "%sAP" ap)]]

      :right [(when (pos? duration-left) [pill duration-left "badge-secondary"])
              (when on-cooldown?         [pill back-in "badge-primary"])]

      :children [[:p.mb-1description description]
                 (render-additional-markup additional-markup)
                 [:hr]
                 [kv "Cooldown" cooldown]]]]))

(defn ^:private passive-li
  [{:keys [name description]}]
  [:div.list-group-item.list-group-item-action.flex-column.align-items-start
   {:style {:padding 0, :margin-bottom "0.25em"}}
   [accordion
    name
    :children [[:p.mb-1description description]]]])

(defn ^:private passives-block
  [passives]
  (when (seq passives)
    [:<>
     [:hr]
     [:h6 "Passives"]
     [:div.list-group.list-group-flush
      (for [passive passives]
        ^{:key (:id passive)}
        [passive-li passive])]]))

(defn ^:private abilities-block
  [char abilities]
  (when (seq abilities)
    [:<>
     [:hr]
     [:h6 "Abilities"]
     [:div.list-group.list-group-flush
      (for [ability abilities]
        ^{:key (:id ability)}
        (ability-li char ability))]]) )

(defn ^:private info-block
  [{:keys [name faction description health dt ap-left ap interleaved?]}]
  [:<>
   [:h5.card-title name]
   [:h6.card-subtitle.text-muted.mb-2 (cuerdas/title faction)]
   [:p.card-text description]
   [:ul.list-unstyled
    [:li [kv "Health" health]]
    [:li [kv "DT" dt]]
    [:li [kv "AP" (gstring/format "%d / %d" ap (or ap-left ap))]]
    (when interleaved?
      [:li [:em "Interleaved"]])]])

(defn character []
  (when-let* [{:keys [passives abilities] :as char}
              @(rf/subscribe [::subs/selected-character])]
    [:<>
     [:h4.text-center "Character Info"]
     [:div.card
      [:div.card-body
       [info-block char]
       [passives-block passives]
       [abilities-block char (vals abilities)]]]]))

;; ** Timeline
(defn line
  [x1 y1 x2 y2]
  [:line {:style {:stroke "black", :stroke-width 1, :fill "none"}
          :x1 x1 :x2 x2 :y1 y1 :y2 y2}])

(defn circle
  [cx cy r]
  [:circle {:style {:stroke "black" :stroke-width 1 :fill "none"}
            :cx cx, :cy cy, :r r}])

(defn ^:private round-title
  [label]
  [:div.d-flex.w-100.justify-content-center
   [:div.pr-2
    [:svg {:width "40px" :height "13px" :style {:stroke-opacity 0.5}}
     [line 0 6 35 6]
     [circle (+ 35 4) 6 5]]]
   [:div.text-center label]
   [:div.pl-2
    [:svg {:width "40px" :height "13px" :style {:stroke-opacity 0.5}}
     [line 4 6 35 6]
     [circle 0 6 5]]]])

(defn ^:private round-ability
  [[char {:keys [id name]}]]
  (let [{hl-char-id :character/id, hl-ability-id :ability/id}
        @(rf/subscribe [::subs/highlighted-ability])]
    [:a.list-group-item.list-group-item-action.flex-column.align-items-start
    {:class [(when (and (= char hl-char-id) (= hl-ability-id id)) "bg-light")]
     :style {:padding "0.1em"
             :transition "background 0.25s"}}
    [:div.d-flex.w-100.justify-content-center
     [:p.mb-1 name]]]))

(defn ^:private timeline-section
  [[round abilities]]
  [:div
   [round-title round]
   [:div.list-group.list-group-flush
    (for [[_ {id :id} :as ability] abilities]
      ^{:key id}
      [round-ability ability])]])

(defn timeline []
  (let [on-cooldown @(rf/subscribe [::subs/abilities-on-cooldown])

        {character-id :character/id, ability-id :ability/id}
        @(rf/subscribe [::subs/highlighted-ability])

        button (fn [key label]
                 [:button.btn.btn-primary
                  {:type     "button"
                   :on-click #(rf/dispatch [key])}
                  label])]
    [:<>
     [:div.row
      [:div.col
       [:h4.text-center "Timeline"]]]
     [:div.row.d-flex.justify-content-around
      [button :increment-round "Round"]
      [button :increment-interleaved-round "Interleave"]]
     [:hr]
     [:div.row
      [:div.col
       [:div.list-group
        (for [[round chunk] on-cooldown]
          ^{:key round}
          [timeline-section [round chunk]])]]]]))

;; ** Render
(defn render []
  [:<>
   [:div.col-md
    [character-selector]]
   [:div.col-md-6
    [character]]
   [:div.col-md
    [timeline]]])
