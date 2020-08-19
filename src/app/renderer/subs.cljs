(ns app.renderer.subs
  (:require [re-frame.core :as rf]
            [com.rpl.specter :as sp]))

(rf/reg-sub
 ::faction-info
 (fn [db [_ faction-id]]
   (get-in db [:factions faction-id])))

(rf/reg-sub
 ::faction-color
 (fn [[_ faction]]
   (rf/subscribe [::faction-info faction]))
 (fn [faction-info]
   (:color faction-info)))

(rf/reg-sub
 ::templates
 (fn [db _]
   (:templates db)))

(rf/reg-sub
 ::characters
 (fn [db _]
   (vals (:characters db))))

(rf/reg-sub
 ::character-info
 (fn [db [_ character-id]]
   (get-in db [:characters character-id])))

(rf/reg-sub
 ::duplicate-character-name?
 (fn []
   (rf/subscribe [::characters]))
 (fn [characters [_ name]]
   (boolean (some #(= name (:name %)) characters))))

(rf/reg-sub
 ::selected-character
 (fn [db _]
   (get-in db [:selections :current-character])))

(rf/reg-sub
 ::selected-character-info
 (fn []
   (rf/subscribe [::selected-character]))
 (fn [character-id]
   @(rf/subscribe [::character-info character-id])))

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
 (fn [[_ character-id ability-id]]
   (rf/subscribe [::character-info character-id]))
 (fn [character [_ _ ability-id]]
   (pos? (get-in character [:abilities ability-id :back-in]))))

(rf/reg-sub
 ::highlighted-ability
 (fn [db _]
   (get-in db [:selections :highlighted])))

(rf/reg-sub
 ::tab
 (fn [db _]
   (get-in db [:selections :tab])))
