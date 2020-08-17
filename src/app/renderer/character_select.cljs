(ns app.renderer.character-select
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [re-com.core :refer [single-dropdown input-text button]]
            [app.renderer.subs :as subs]
            [cuerdas.core :as cuerdas]
            [app.renderer.events :as events]))

(defn empty-str? [s]
  (or (cuerdas/blank? s)
      (cuerdas/empty-or-nil? s)))

;; * Character Selector
;; ** Add Character Form
(defn ^:private character-from-template
  [name t]
  (-> t
      (assoc :name name)
      (assoc :uuid (uuid name))) )

(defn add-character []
  (let [templates              (or @(rf/subscribe [::subs/templates]) [])
        selected-template-id   (r/atom nil)
        template-name-override (r/atom nil)]
    (fn []
      (let [selected-template (->> templates
                                 (filter #(= (:id %) @selected-template-id))
                                 first)
            placeholder-name  (get selected-template :name)

            template-name     (if (empty-str? @template-name-override)
                                placeholder-name
                                @template-name-override)
            submittable?      (not (or (nil? @selected-template-id)
                                       @(rf/subscribe [::subs/duplicate-character-name? template-name])))]
        [:form
         [:h4.text-center "Add Characters"]
         [:div.form-group
          [:label "Template"]
          [single-dropdown
           :choices templates
           :model selected-template-id
           :label-fn :name
           :placeholder "Select a template"
           :class "w-100"
           :on-change #(reset! selected-template-id %)]]
         [:div.form-group
          [:label "Name"]
          [input-text
           :model template-name-override
           :on-change #(reset! template-name-override %)
           :change-on-blur? false
           :width "100%"
           :placeholder placeholder-name]]
         [button
          :attr {:disabled ""}
          :class (str "btn " (if submittable? "btn-primary" "btn-secondary"))
          :label "Add Character"
          :disabled? (not submittable?)
          :on-click (fn []
                      (let [c (character-from-template template-name selected-template)]
                        (rf/dispatch [:add-character c])))]]))))

;; ** Active Characters
(defn ^:private character
  [{:keys [uuid name]} selected?]
  [:a.list-group-item.list-group-item-action.flex-column.align-items-start
   {:href "#"
    :class [(if selected? "active")]
    :style {:cursor "default"}}
   [:div.d-flex.w-100.justify-content-between
    [:div.p-1 [:i.fa.fa-fw.fa-times.text-danger
               {:style    {:transform "scale(1.5,1.5)", :cursor "pointer"}
                :on-click #(rf/dispatch [:remove-character uuid])}]]

    [:h5
     {:on-click #(rf/dispatch [:set-selected-character-id uuid])}
     name]]])

(defn active-characters []
  (let [characters       @(rf/subscribe [::subs/characters])
        active-character @(rf/subscribe [::subs/selected-character-id])]
    [:div
     [:h4.text-center "Characters"]
     [:div.list-group.list-group-flush
      (for [c characters
            :let [selected? (= uuid active-character)
                  key (:uuid c)]]
        ^{:key key}
        [character c selected?])]]))

;; ** Render
(defn render []
  [:<>
   [:div.col-md
    [add-character]]
   [:div.col-md-4]
   [:div.col-md
    [active-characters]]])
