(ns app.renderer.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 ::templates
 (fn [db _]
   (:templates db)))

(rf/reg-sub
 ::characters
 (fn [db _]
   (vals (:characters db))))

(rf/reg-sub
 ::duplicate-character-name?
 (fn [db [_ name]]
   (not (empty? (filter #(= name (:name %)) (:characters db))))))

(rf/reg-sub
 ::selected-character-id
 (fn [db _]
   (:selected-character-id db)))

(rf/reg-sub
 ::selected-character
 (fn [db _]
   (let [id (:selected-character-id db)]
     (get (:characters db) id))))

(defn spy [x] (println x) x)

(rf/reg-sub
 ::abilities-on-cooldown
 (fn [db _]
   (let [on-cooldown (for [{:keys [uuid abilities]} (-> db :characters vals)
                           ability (vals abilities)
                           :when (pos? (get ability :back-in))]
                       [uuid ability])
         on-cooldown (->> on-cooldown
                          (sort-by :back-in)
                          (group-by #(-> % second :back-in)))
         last-round (or (apply max (keys on-cooldown)) 0)
         defaults (into {} (map-indexed vector (repeat last-round '())))
         ]
     (dissoc (into (sorted-map) (merge-with concat on-cooldown defaults)) 0))))

(rf/reg-sub
 ::ability-on-cooldown?
 (fn [db [_ char-id ability-id]]
   (pos? (get-in db [:characters char-id :abilities ability-id :back-in]))))

(rf/reg-sub
 ::highlighted-ability
 (fn [db _]
   (:highlighted-ability db)))

(rf/reg-sub
 ::tab
 (fn [db _]
   (:tab db)))
