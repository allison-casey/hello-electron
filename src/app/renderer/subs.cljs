(ns app.renderer.subs
  (:require [re-frame.core :as rf]
            [com.rpl.specter :as sp]))

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
   (sp/selected-any? [:characters sp/MAP-VALS #(= name (:name %))] db)))

(rf/reg-sub
 ::selected-character-id
 (fn [db _]
   (:selected-character-id db)))

(rf/reg-sub
 ::selected-character
 (fn [db _]
   (let [id (:selected-character-id db)]
     (get (:characters db) id))))

(rf/reg-sub
 ::abilities-on-cooldown
 (fn [db _]
   (let [on-cooldown
         (->> db
            (sp/select [:characters
                        sp/MAP-VALS
                        (sp/collect-one :uuid)
                        :abilities
                        sp/MAP-VALS
                        #(-> % :back-in pos?)])
            (group-by #(-> % second :back-in)))

         ;; This is to fill in the precedings rounds that have no
         ;; abilities to fill them so rounds [2, 3] become [1, 2, 3]
         last-round (or (apply max (keys on-cooldown)) 0)
         defaults (into {} (map-indexed vector (repeat last-round '())))]

     ;; This way leaves a 0 in the map which we need to remove since there
     ;; is no such thing as 0 rounds left
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
