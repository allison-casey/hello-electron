(ns app.renderer.combat-tracker.character-selector
  (:require [re-frame.core :as rf]
            [app.renderer.subs :as subs]))


(defn ^:private character-item
  [{:keys [uuid name faction]
    {health-left :health-left} :tracker/internal
    :as character} active-character]
  (let [faction-color @(rf/subscribe [::subs/faction-color faction])]
    [:li.list-group-item
     {:on-click #(rf/dispatch [:set-selected-character-id uuid])
      :class ["list-group-item" (if (= uuid active-character) "active")]
      :style {:background faction-color}}
     name (when (<= health-left 0)
            [:img.ml-2 {:src "img/skull-crossbones.png"
                        :width "16em"
                        :height "16em"}])]))

(defn render []
  (let [characters @(rf/subscribe [::subs/characters])
        active-character @(rf/subscribe [::subs/selected-character])]
    [:<>
     [:div.row [:div.col [:h4.text-center "Characters"]]]
     [:div.row
      [:div.col
       [:ul.list-group
        (for [character characters]
          ^{:key (:uuid character)}
          [character-item character active-character])]]]]))
