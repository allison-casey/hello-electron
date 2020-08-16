(ns app.renderer.combat-tracker
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [re-com.core :refer [horizontal-tabs single-dropdown input-text button]]
            [app.renderer.subs :as subs]
            [app.renderer.events :as events]
            [goog.string :as gstring]
            [goog.string.format]
            [cuerdas.core :as cuerdas]
            [app.renderer.macros :refer [when-let*]]))

(defn character-selector []
  (let [characters @(rf/subscribe [::subs/characters])
        active-character @(rf/subscribe [::subs/selected-character-id])]
    [:<>
     [:div.row [:div.col [:h4 "Characters"]]]
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
            (when (seq left) (into [:div] left))
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

(defn kv [key value]
  [:p.m-0 [:strong key] " : " value])

(defn ^:private ability-li
  [char-id {:keys [id name description cooldown back-in] :as ability}]
  (let [on-cooldown? (zero? (or back-in 0))]
    [:div.list-group-item.list-group-item-action.flex-column.align-items-start
     {:on-mouse-enter #(rf/dispatch [:set-highlighted-ability char-id id])
      :on-mouse-leave #(rf/dispatch [:remove-highlighted-ability])
      :style {:padding 0
              :margin-bottom "0.25em"}}
     [accordion name
      :left [[:i.fa.fa-fw {:style {:cursor (if on-cooldown? "pointer" "not-allowed")}
                           :class [(if on-cooldown? "fa-check-circle-o" "fa-times-circle-o")]
                           :on-click (fn [event]
                                       (rf/dispatch [:use-ability char-id ability])
                                       (.preventDefault event))}]]
      :right [(when-not on-cooldown? [:span.badge.badge-primary.badge-pill back-in])]
      :children [[:p.mb-1description description]
                 [kv "Cooldown" cooldown]]]]))

(defn ^:private passive-li
  [{:keys [name description]}]
  [:div.list-group-item.list-group-item-action.flex-column.align-items-start
   {:style {:padding 0
            :margin-bottom "0.25em"}}
   [accordion name
    :children [[:p.mb-1description description]]]])

(defn character []
  (when-let* [{:keys [uuid name faction description abilities passives health dt] :as x} @(rf/subscribe [::subs/selected-character])
              ;; {char-id :character-id ability-id :ability-id} @(rf/subscribe [::subs/highlighted-ability])
              ]
    (let [abilities-lis (for [ability (vals abilities)]
                          ^{:key (:id ability)}
                          (ability-li uuid ability))
          passives-lis (for [passive passives]
                         ^{:key (:id passives)}
                         (passive-li passive))]
      [:<>
       [:h4 "Character Info"]
       [:div.card
        [:div.card-body
         [:h5.card-title name]
         [:h6.card-subtitle.text-muted.mb-2
          (cuerdas/phrase faction)]
         [:p.card-text description]
         [:ul.list-unstyled
          [:li [kv "Health" health]]
          [:li [kv "DT" dt]]]
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
        {:keys [character-id ability-id]} @(rf/subscribe [::subs/highlighted-ability])]
    [:<>
     [:div.row
      [:div.col
       [:h4 "Timeline"]
       [:h5 "Advance Cooldowns"]]]
     [:div.row
      [:div.col
       [:button.btn.btn-primary
        {:type "button"
         :on-click #(rf/dispatch [:increment-round])}
        "Round"]]
      [:div.col
       [:button.btn.btn-secondary
        {:type "button"
         :on-click #(rf/dispatch [:increment-interleaved-round])}
        "Interleave"]]]
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
            (for [[char-id {:keys [id name]}] chunk]
              ^{:key id}
              [:a.list-group-item.list-group-item-action.flex-column.align-items-start
               {:class [(when (and (= char-id character-id) (= ability-id id)) "bg-info")]
                :style {"padding" "0.1em"}}
               [:div.d-flex.w-100.justify-content-center
                [:p.mb-1 name]]])]])]]]]))

(defn render []
  [:<>
   [:div.col
    [character-selector]]
   [:div.col-6
    [character]]
   [:div.col
    [timeline]]]
  )
