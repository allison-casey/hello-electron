(ns app.renderer.character-select
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [re-com.core :refer [horizontal-tabs single-dropdown input-text button]]
            [app.renderer.subs :as subs]
            [cuerdas.core :as cuerdas]
            [app.renderer.events :as events]))

(defn add-character []
  (let [templates (or @(rf/subscribe [::subs/templates]) [])
        selected-template-id (r/atom nil)
        template-name-override (r/atom nil)]
    (fn []
      (let [selected-template (first (filter #(= (:id %) @selected-template-id) templates))
            placeholder-name (get selected-template :name)
            template-name (if (or (cuerdas/blank? @template-name-override)
                                  (cuerdas/empty-or-nil? @template-name-override))
                            placeholder-name
                            @template-name-override)
            submittable? (not (or (nil? @selected-template-id)
                                  @(rf/subscribe [::subs/duplicate-character-name? template-name])))]
        [:form
         [:h4 "Add Characters"]
         [:div.form-group
          [:label "Template"]
          [single-dropdown
           :choices templates
           :model selected-template-id
           :label-fn :name
           :placeholder "Select a template"
           :width "300px"
           :on-change #(reset! selected-template-id %)]]
         [:div.form-group
          [:label "Name"]
          [input-text
           :model template-name-override
           :on-change #(reset! template-name-override %)
           :change-on-blur? false
           :placeholder placeholder-name]]
         [button
          :attr {:disabled ""}
          :class (str "btn " (if submittable? "btn-primary" "btn-secondary"))
          :label "Add Character"
          :disabled? (not submittable?)
          :on-click (fn []
                      (let [character (-> selected-template
                                         (assoc :name template-name)
                                         (assoc :uuid (uuid template-name)))]
                        (rf/dispatch
                         [:add-character character])))]]))))

(defn active-characters []
  (let [characters @(rf/subscribe [::subs/characters])
        active-character @(rf/subscribe [::subs/selected-character-id])]
    [:div
     [:h4.text-right "Characters"]
     [:ul.list-group
      (for [{:keys [uuid name]} characters]
        ^{:key uuid}
        [:li
         {:on-click #(rf/dispatch [:set-selected-character-id uuid])
          :class ["list-group-item" (if (= uuid active-character) "active")]}
         name])]]))

(defn render []
  [:<>
   [:div.col
    [add-character]]
   [:div.col
    [active-characters]]])
