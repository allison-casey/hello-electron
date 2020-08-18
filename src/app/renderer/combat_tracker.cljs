(ns app.renderer.combat-tracker
  (:require [app.renderer.combat-tracker.timeline :as timeline]
            [app.renderer.combat-tracker.character-selector :as char-select]
            [app.renderer.combat-tracker.character-info :as char-info]))

;; ** Render
(defn render []
  [:<>
   [:div.col-md
    [char-select/render]]
   [:div.col-md-6
    [char-info/render]]
   [:div.col-md
    [timeline/render]]])
