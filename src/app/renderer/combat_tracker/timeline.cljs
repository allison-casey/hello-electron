(ns app.renderer.combat-tracker.timeline
  (:require [re-frame.core :as rf]
            [app.renderer.subs :as subs]))

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
  (let [{hl-char-id :character, hl-ability-id :ability}
        @(rf/subscribe [::subs/highlighted-ability])]
    [:a.list-group-item.list-group-item-action.flex-column.align-items-start
    {:class [(when (and (= char hl-char-id) (= hl-ability-id id)) "bg-light")]
     :style {:padding "0.1em", :transition "background 0.25s"}}
    [:div.d-flex.w-100.justify-content-center
     [:p.mb-1 name]]]))

(defn ^:private timeline-section
  [[round abilities]]
  [:div
   [round-title round]
   [:div.list-group.list-group-flush
    (for [[char-id ability] abilities]
      ^{:key (:id ability)}
      [round-ability char-id ability])]])

(defn render []
  (let [on-cooldown @(rf/subscribe [::subs/abilities-on-cooldown])
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
      [button :increment-interleaved-round "Turn"]]
     [:hr]
     [:div.row
      [:div.col
       [:div.list-group
        (for [[round chunk] on-cooldown]
          ^{:key round}
          [timeline-section [round chunk]])]]]]))
