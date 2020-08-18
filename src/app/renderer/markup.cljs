;; %%
(ns app.renderer.markup
  (:require [app.renderer.components :refer [kv to-clipboard]]
            [clojure.walk :refer [postwalk]]))

(defmulti markup :type)

(defmethod markup "ul"
  [{:keys [items]}]
  [:ul.list-unstyled
   (for [item items]
     ^{:key (random-uuid)}
     [:li item])])

(defmethod markup "kv"
  [{:keys [key value]}]
  [kv key value])

(defmethod markup "roll"
  [{:keys [label roll]}]
  [:<>
   [:span label]
   [:img.ml-2 {:src "img/dice.png"
               :on-click #(to-clipboard (str "/roll " roll))
               :width "16em"
               :height "16em"
               :style {:cursor "pointer"}}]])

(defmethod markup :default
  [arg]
  arg)

(defn render [coll]
  (postwalk markup coll))
