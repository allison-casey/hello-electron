(ns app.renderer.combat-tracker
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [re-com.core :refer [horizontal-tabs single-dropdown input-text button]]
            [app.renderer.subs :as subs]
            [app.renderer.events :as events]
            [goog.string :as gstring]
            [goog.string.format]
            [app.renderer.macros :refer [when-let*]]))


(defn ability-li [selected? char-id {:keys [id name description cooldown] :as ability}]
  [:li
   {:class [(when selected? "bg-info")]}
   [:div
    {:on-mouse-enter #(rf/dispatch [:set-highlighted-ability char-id id])
     :on-mouse-leave #(rf/dispatch [:remove-highlighted-ability])}
    [button
     :label "Use"
     :disabled? @(rf/subscribe [::subs/ability-on-cooldown? char-id id])
     :on-click #(rf/dispatch [:use-ability char-id ability])]
    (gstring/format "Name: %s, Description: %s, Cooldown: %d" name description cooldown)]])

(defn character-info []
  (let [character @(rf/subscribe [::subs/selected-character])
        {char-id :character-id ability-id :ability-id} @(rf/subscribe [::subs/highlighted-ability])]
    (when-let [{:keys [name description abilities passives]} character]
      [:div
       [:h4 "Character Info"]
       [:h5 "Name:" name]
       [:div "Description: " description]
       [:h6 "Passives:"]
       [:ul
        (for [{:keys [id name description]} passives]
          ^{:key id}
          [:li name ": " description])]
       [:h6 "Abilities:"]
       [:ul
        (doall (for [[id ability] abilities]
                 ^{:key id}
                 [ability-li
                  (and (= char-id (:uuid character)) (= ability-id (:id ability)))
                  (:uuid character)
                  ability]))]])))

(defn active-characters []
  (let [characters @(rf/subscribe [::subs/characters])
        active-character @(rf/subscribe [::subs/selected-character-id])]
    [:div
     [:h4 "Characters"]
     [:ul.list-group
      (for [{:keys [uuid name]} characters]
        ^{:key uuid}
        [:li
         {:on-click #(rf/dispatch [:set-selected-character-id uuid])
          :class ["list-group-item" (if (= uuid active-character) "active")]}
         name])]]))

(defn ability-timeline []
  (let [on-cooldown @(rf/subscribe [::subs/abilities-on-cooldown])
        {:keys [character-id ability-id]} @(rf/subscribe [::subs/highlighted-ability])]
    [:div
     [:h4 "Ability Timeline"]
     (for [[round chunk] on-cooldown]
       ^{:key round}
       [:div
        [:h5 "Round: " round]
        [:ul
         (for [[char-id {:keys [id name]}] chunk]
           ^{:key id}
           [:li
            {:class [(when (and (= char-id character-id) (= ability-id id)) "bg-info")]}
            name])]])]))

;;; Refactored
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

(defn character []
  (letfn [(ability-li [char-id {:keys [id name description cooldown back-in] :as ability}]
            [:a.list-group-item.list-group-item-action.flex-column.align-items-start
             {:on-click (fn [event]
                          (rf/dispatch [:use-ability char-id ability])
                          (.preventDefault event))
              :on-mouse-enter #(rf/dispatch [:set-highlighted-ability char-id id])
              :on-mouse-leave #(rf/dispatch [:remove-highlighted-ability])}
             [:div.d-flex.w-100.justify-content-between
              [:h5.mb-1 name]
              (when-not (zero? (or back-in 0)) [:span.badge.badge-primary.badge-pill back-in])]
             [:p.mb-1description description]
             [:p [:strong "Cooldown"] " : " cooldown]])
          (passive-li [{:keys [name description]}]
            [:div.list-group-item.list-group-item-action.flex-column.align-items-start
             {:href "#"}
             [:div.d-flex.w-100
              [:h5.mb-1 name]]
             [:p.mb-1description description]])]
    (when-let* [{:keys [uuid name faction description abilities passives] :as x} @(rf/subscribe [::subs/selected-character])
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
           [:h6.card-subtitle.text-muted faction]
           [:p.card-text description]
           (when (seq passives-lis)
             [:<>
              [:hr]
              [:h6 "Passives"]
              [:div.list-group.list-group-flush passives-lis]])
           (when (seq abilities-lis)
             [:<>
              [:hr]
              [:h6 "Abilities"]
              [:div.list-group.list-group-flush abilities-lis]])]]]))))

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
           [:h6.text-center "--- " round " ---"]
           [:div.list-group.list-group-flush
            (for [[char-id {:keys [id name]}] chunk]
              ^{:key id}
              [:a.list-group-item.list-group-item-action.flex-column.align-items-start
               {:class [(when (and (= char-id character-id) (= ability-id id)) "bg-info")]
                :style {"padding" "0.1em"}}
               [:div.d-flex.w-100.justify-content-between
                [:p.mb-1 name]]])]])]]]]))

(defn render []
  [:<>
   [:div.col
    [character-selector]]
   [:div.col-6
    [character]]
   [:div.col
    [timeline]]])
