(ns app.renderer.combat-tracker
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [re-com.core :refer [horizontal-tabs single-dropdown input-text button]]
            [app.renderer.subs :as subs]
            [app.renderer.events :as events]
            [goog.string :as gstring]
            [goog.string.format]
            [com.rpl.specter :as sp]
            [cuerdas.core :as cuerdas]
            [app.renderer.macros :refer [when-let*]]))

(defn kv [key value]
  [:p.m-0 [:strong key] " : " value])

(defn render-additional-markup [markup]
  [:ul.list-unstyled
   (for [{:keys [key value]} markup]
     ^{:key key}
     [:li [kv key value]])])

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

(defn accordion [title & {:keys [children left right]
                          :or {left [] right []}}]
  (let [s (r/atom {:open? false
                   :child-height 0})]
    (fn [title & {:keys [children left right]
                  :or {left [] right []}}]
      (let [{:keys [open? child-height]} @s]
        [:div.accordion
         [:div.card
          [:div.card-header
           [:div.d-flex.w-100 ;;.justify-content-between
            (when (seq left) (into [:div.d-flex] left))
            [:div.flex-grow-1.ml-1.align-middle.mb-0 title]
            (when (seq right) (into [:div] right))
            [:a.ml-1.align-middle
             {:on-click #(swap! s update :open? not)
              :style {:cursor "pointer"}}
             [:i.fa {:class [(if open? "fa-minus" "fa-plus")]}]]]]
          [:div.collapse.show
           {:style {:max-height (if open? child-height 0)
                    :transition "max-height 0.8s"
                    :overflow "hidden"}}
           (into [:div.card-body
                  {:ref #(when % (swap! s assoc :child-height (.-clientHeight %)))}]
                 children)]]]))))

(defn ^:private ability-li
  [char {:keys [id name description cooldown back-in
                   additional-markup duration-left ap] :as ability}]
  (let [on-cooldown? (pos? (or back-in 0))
        disabled? (or on-cooldown? (< (or (:ap-left char) (:ap char)) ap))]
    [:div.list-group-item.list-group-item-action.flex-column.align-items-start
     {:on-mouse-enter #(rf/dispatch [:set-highlighted-ability (:uuid char) id])
      :on-mouse-leave #(rf/dispatch [:remove-highlighted-ability])
      :style {:padding 0
              :margin-bottom "0.25em"}}
     [accordion name
      :left [[:div [:i.fa.fa-fw {:style {:cursor (if disabled? "not-allowed" "pointer")}
                                 :class [(if disabled? "fa-times-circle-o" "fa-check-circle-o")]
                                 :on-click (fn [event]
                                             (when-not disabled?
                                               (rf/dispatch [:use-ability (:uuid char) ability]))
                                             (.preventDefault event))}]]
             [:div.text-muted (gstring/format "%sAP" ap)]]
      :right [(when (pos? duration-left) [:span.badge.badge-secondary.badge-pill duration-left])
              (when on-cooldown? [:span.badge.badge-primary.badge-pill back-in])]
      :children [[:p.mb-1description description]
                 (when additional-markup
                   (render-additional-markup additional-markup))
                 [:hr]
                 [kv "Cooldown" cooldown]]]]))

(defn ^:private passive-li
  [{:keys [name description]}]
  [:div.list-group-item.list-group-item-action.flex-column.align-items-start
   {:style {:padding 0
            :margin-bottom "0.25em"}}
   [accordion name
    :children [[:p.mb-1description description]]]])

(defn character []
  (when-let* [{:keys [uuid name faction description abilities
                      passives health dt interleaved?] :as char}
              @(rf/subscribe [::subs/selected-character])]
    (let [abilities-lis (for [ability (vals abilities)]
                          ^{:key (:id ability)}
                          (ability-li char ability))
          passives-lis (for [passive passives]
                         ^{:key (:id passives)}
                         (passive-li passive))]
      [:<>
       [:h4.text-center "Character Info"]
       [:div.card
        [:div.card-body
         [:h5.card-title name]
         [:h6.card-subtitle.text-muted.mb-2
          (cuerdas/title faction)]
         [:p.card-text description]
         [:ul.list-unstyled
          [:li [kv "Health" health]]
          [:li [kv "DT" dt]]
          [:li [kv "AP" (gstring/format "%d / %d"
                                        (:ap char)
                                        (or (:ap-left char) (:ap char)))]]
          (when interleaved?
            [:li [:em "Interleaved"]])]
         (when (seq passives-lis)
           [:<>
            [:hr]
            [:h6 "Passives"]
            [:div.list-group.list-group-flush passives-lis]])
         (when (seq abilities-lis)
           [:<>
            [:hr]
            [:h6 "Abilities"]
            [:div.list-group.list-group-flush abilities-lis]])]]])))

(defn timeline []
  (let [on-cooldown @(rf/subscribe [::subs/abilities-on-cooldown])
        {character-id :character/id
         ability-id :ability/id} @(rf/subscribe [::subs/highlighted-ability])]
    [:<>
     [:div.row
      [:div.col
       [:h4.text-center "Timeline"]]]
     [:div.row.d-flex.justify-content-around
      [:button.btn.btn-primary
        {:type "button"
         :on-click #(rf/dispatch [:increment-round])}
        "Round"]
      [:button.btn.btn-secondary
        {:type "button"
         :on-click #(rf/dispatch [:increment-interleaved-round])}
        "Interleave"]]
     [:hr]
     [:div.row
      [:div.col
       [:div.list-group
        (for [[round chunk] on-cooldown]
          ^{:key round}
          [:div
           [:div.d-flex.w-100.justify-content-center
            [:div.pr-2
             [:svg {:width "40px" :height "13px" :style {:stroke-opacity 0.5}}
              [:line {:style {:stroke "black" :stroke-width 1} :x1 0 :x2 35 :y1 6 :y2 6}]
              [:circle {:style {:stroke "black" :stroke-width 1 :fill "none"}
                        :cx (+ 35 4) :cy 6 :r 5} ]]]
            [:div.text-center round]
            [:div.pl-2
             [:svg {:width "40px" :height "13px" :style {:stroke-opacity 0.5}}
              [:line {:style {:stroke "black" :stroke-width 1} :x1 4 :x2 35 :y1 6 :y2 6}]
              [:circle {:style {:stroke "black" :stroke-width 1 :fill "none"}
                        :cx 0 :cy 6 :r 5} ]]]]
           [:div.list-group.list-group-flush
            (for [[char {:keys [id name]}] chunk]
              ^{:key id}
              [:a.list-group-item.list-group-item-action.flex-column.align-items-start
               {:class [(when (and (= char character-id) (= ability-id id)) "bg-light")]
                :style {:padding "0.1em"
                        :transition "background 0.25s"}}
               [:div.d-flex.w-100.justify-content-center
                [:p.mb-1 name]]])]])]]]]))

(defn render []
  [:<>
   [:div.col-md
    [character-selector]]
   [:div.col-md-6
    [character]]
   [:div.col-md
    [timeline]]]
  )
