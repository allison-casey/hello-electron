(ns app.renderer.components
  (:require [reagent.core :as r]
            [re-com.core :refer [modal-panel]]))

(defn icon
  [fa-class & {:keys [cursor on-click style]}]
  [:div
   [:i.fa.fa-fw
    {:style    (merge style {:cursor cursor})
     :on-click on-click
     :class    [fa-class]}]])

(defn kv [key value]
  [:p.m-0 [:strong key] " : " value])

(defn bar [] [:span.text-muted \u007C])

(defn pill [number badge-color]
  [:span.badge.badge-pill
   {:class [badge-color]}
   number])

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

(defn to-clipboard [text]
  (.writeText js/navigator.clipboard text))

(defn clipboard-button
  [class string]
  (let [showing? (r/atom false)]
    (fn [class string]
      [:<>
       [icon
        class
        :cursor   "pointer"
        :on-click (fn []
                    (reset! showing? true)
                    (js/setTimeout #(reset! showing? false) 1000)
                    (to-clipboard string))]
       (when @showing?
         [modal-panel
          :backdrop-on-click #(reset! showing? false)
          :child [:span "Copied!"]])])))
