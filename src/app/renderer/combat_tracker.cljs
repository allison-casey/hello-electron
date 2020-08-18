(ns app.renderer.combat-tracker
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [re-com.core :refer [modal-panel]]
            [app.renderer.subs :as subs]
            [app.renderer.events :as events]
            [goog.string :as gstring]
            [goog.string.format]
            [com.rpl.specter :as sp]
            [cuerdas.core :as cuerdas]
            [app.renderer.macros :refer [when-let*]]
            [app.renderer.markup :as markup]
            [app.renderer.components :refer [pill
                                             icon
                                             clipboard-button
                                             kv
                                             accordion
                                             bar]]))

;; * Combat Tracker
;; ** Utility Elements
;; (defn kv [key value]
;;   [:p.m-0 [:strong key] " : " value])

;; (defn bar [] [:span.text-muted \u007C])

;; (defn to-clipboard [text]
;; ** Character Selector

(defn ^:private character-item
  [{:keys [uuid name health-left faction] :as character} active-character]
  (let [faction-color @(rf/subscribe [::subs/faction-color faction])]
    [:li.list-group-item
     {:on-click #(rf/dispatch [:set-selected-character-id uuid])
      :class ["list-group-item" (if (= uuid active-character) "active")]
      :style {:background faction-color}}
     name (when (<= health-left 0)
            [:img.ml-2 {:src "img/skull-crossbones.png"
                        :width "16em"
                        :height "16em"}])]))

(defn character-selector []
  (let [characters @(rf/subscribe [::subs/characters])
        active-character @(rf/subscribe [::subs/selected-character-id])]
    [:<>
     [:div.row [:div.col [:h4.text-center "Characters"]]]
     [:div.row
      [:div.col
       [:ul.list-group
        (for [character characters]
          ^{:key (:uuid character)}
          [character-item character active-character])]]]]))

;; ** Character Info Card

;; (defn clipboard-button
;;   [class string]
;;   (let [showing? (r/atom false)]
;;     (fn [class string]
;;       [:<>
;;        [icon
;;         class
;;         :cursor   "pointer"
;;         :on-click (fn []
;;                     (reset! showing? true)
;;                     (js/setTimeout #(reset! showing? false) 1000)
;;                     (to-clipboard string))]
;;        (when @showing?
;;          [modal-panel
;;           :backdrop-on-click #(reset! showing? false)
;;           :child [:span "Copied!"]])])))

(defn spy [x] (cljs.pprint/pprint x) x)
(defn ^:private ability-li
  [char {:keys [id name description cooldown back-in
                additional-markup duration-left ap
                accuracy damage] :as ability}]
  (let [on-cooldown? (pos? (or back-in 0))
        enough-ap?   (>= (or (:ap-left char) (:ap char)) ap)
        dead? (<= (:health-left char) 0)
        disabled?    (or dead? on-cooldown? (not enough-ap?))]
    [:div.list-group-item.list-group-item-action.flex-column.align-items-start
     {:on-mouse-enter #(rf/dispatch [:set-highlighted-ability (:uuid char) id])
      :on-mouse-leave #(rf/dispatch [:remove-highlighted-ability])
      :style          {:padding 0, :margin-bottom "0.25em"}}
     [accordion
      name
      :left [;; Use ability button
             [icon
              (if disabled? "fa-times-circle-o" "fa-check-circle-o")
              :cursor (if disabled? "not-allowed" "pointer")
              :on-click  (fn [event]
                           (when-not disabled?
                             (rf/dispatch
                              [:use-ability (:uuid char) ability]))
                           (.preventDefault event))]
             (when (or accuracy damage)
               [:<>
                [bar]
                ;; Copy accuracy button
                (when accuracy
                  [clipboard-button "fa-crosshairs" (gstring/format "/roll %s" accuracy)])
                ;; Copy damage button
                (when damage
                  [clipboard-button "fa-bomb" (gstring/format "/roll %s" damage)])
                [bar]])
             ;; AP Cost
             [:div.text-muted (gstring/format "%sAP" ap)]]

      :right [(when (pos? duration-left) [pill duration-left "badge-secondary"])
              (when on-cooldown?         [pill back-in "badge-primary"])]

      :children [[:p.mb-1description description]
                 (markup/render additional-markup)
                 ;; (render-additional-markup additional-markup)
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
  [{:keys [name faction description health health-left
           dt ap-left ap interleaved? uuid]}]
  (letfn [(btn [class key]
            [icon class
             :cursor "pointer"
             :on-click #(rf/dispatch [key uuid])])
          (scroll-dispatch [e]
            (if (pos? (.-deltaY e))
              (rf/dispatch [:dec-health-left uuid])
              (rf/dispatch [:inc-health-left uuid])))]
    [:<>
     [:h5.card-title.d-flex name (when (<= health-left 0)
                                   [:img.ml-2 {:src "img/skull-crossbones.png"
                                               :width "16em"
                                               :height "16em"}])]
    [:h6.card-subtitle.text-muted.mb-2 (cuerdas/title faction)]
    [:p.card-text description]
    [:ul.list-unstyled
     [:li [:div.d-flex
           [:div {:on-wheel scroll-dispatch}
            [kv "Health" (gstring/format "%d / %d" health-left health)]]
           [btn "fa-caret-square-o-up" :inc-health-left]
           [btn "fa-caret-square-o-down" :dec-health-left]]]
     [:li [kv "DT" dt]]
     [:li [:div.d-flex
           [kv "AP" (gstring/format "%d / %d" ap-left ap)]
           [btn "fa-caret-square-o-up" :inc-ap-left]
           [btn "fa-caret-square-o-down" :dec-ap-left]]]
     (when interleaved?
       [:li [:em "Interleaved" ]])]]))

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
          :x1 x1, :x2 x2, :y1 y1, :y2 y2}])

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
  [char {:keys [id name]}]
  (let [{hl-char-id :character/id, hl-ability-id :ability/id}
        @(rf/subscribe [::subs/highlighted-ability])]
    [:a.list-group-item.list-group-item-action.flex-column.align-items-start
    {:class [(when (and (= char hl-char-id) (= hl-ability-id id)) "bg-light")]
     :style {:padding "0.1em", :transition "background 0.25s"}}
    [:div.d-flex.w-100.justify-content-center
     [:p.mb-1 name]]]))

(defn ^:private timeline-section
  [char [round abilities]]
  [:div
   [round-title round]
   [:div.list-group.list-group-flush
    (for [[_ ability] abilities]
      ^{:key (:id ability)}
      [round-ability char ability])]])

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
          [timeline-section character-id [round chunk]])]]]]))

;; ** Render
(defn render []
  [:<>
   [:div.col-md
    [character-selector]]
   [:div.col-md-6
    [character]]
   [:div.col-md
    [timeline]]])
