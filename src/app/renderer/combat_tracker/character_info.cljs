(ns app.renderer.combat-tracker.character-info
  (:require [re-frame.core :as rf]
            [goog.string :as gstring]
            [goog.string.format]
            [cuerdas.core :as cuerdas]
            [app.renderer.macros :refer [when-let*]]
            [app.renderer.markup :as markup]
            [app.renderer.subs :as subs]
            [app.renderer.components :refer [pill
                                             icon
                                             clipboard-button
                                             kv
                                             accordion
                                             bar]]))


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

(defn render []
  (when-let* [{:keys [passives abilities] :as char}
              @(rf/subscribe [::subs/selected-character])]
    [:<>
     [:h4.text-center "Character Info"]
     [:div.card
      [:div.card-body
       [info-block char]
       [passives-block passives]
       [abilities-block char (vals abilities)]]]]))
