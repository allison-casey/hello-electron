(ns app.renderer.events
  (:require [re-frame.core :as rf]
            [com.rpl.specter :as sp]))

(rf/reg-event-db
 :initialize
 (fn [_ _]
   {:templates []
    :characters {}
    :faction-colors {"spartans" "#d1daff"
                     "bandits" "#ffa49e"
                     "independent" "#95a341"
                     "the-dead-hand" "#919191"
                     "monster" "#a38f97"}
    :selected-character-id nil
    :tab :character-select
    :highlighted-ability {:character/id nil
                          :ability/id nil}}))

(rf/reg-event-db
 :initialize-templates
 (fn [db [_ templates]]
   (assoc db :templates templates)))

(rf/reg-event-db
 :add-character
 (fn [db [_ character]]
   (update db :characters assoc (:uuid character) character)))

(rf/reg-event-db
 :remove-character
 (fn [db [_ character-uuid]]
   (update db :characters dissoc character-uuid)))

(rf/reg-event-db
 :set-selected-character-id
 (fn [db [_ character-uuid]]
   (assoc db :selected-character-id character-uuid)))

(rf/reg-event-db
 :use-ability
 (fn [db [_ character-uuid {:keys [id cooldown ap duration]}]]
   (let [character (get-in db [:characters character-uuid])
         ability (-> (get-in character [:abilities id])
                    (assoc :back-in cooldown)
                    (cond-> duration (assoc :duration-left duration)))
         character (-> character
                      (assoc :ap-left (- (or (:ap-left character)
                                             (:ap character))
                                         ap))
                      (assoc-in [:abilities id] ability))]
     (assoc-in db [:characters character-uuid] character))))

(rf/reg-event-db
 :increment-round
 (fn [db _]
   (->> db
      (sp/transform [:characters sp/MAP-VALS :abilities sp/MAP-VALS :back-in pos?] dec)
      (sp/transform [:characters sp/MAP-VALS :abilities sp/MAP-VALS :duration-left pos?] dec)
      (sp/transform [:characters sp/MAP-VALS] #(assoc % :ap-left (:ap %))))))

(rf/reg-event-db
 :increment-interleaved-round
 (fn [db _]
   (->> db
      (sp/transform [:characters sp/MAP-VALS #(:interleaved? %) :abilities sp/MAP-VALS :back-in pos?] dec)
      (sp/transform [:characters sp/MAP-VALS #(:interleaved? %) :abilities sp/MAP-VALS :duration-left pos?] dec)
      (sp/transform [:characters sp/MAP-VALS #(:interleaved? %)] #(assoc % :ap-left (:ap %))))))

(rf/reg-event-db
 :set-highlighted-ability
 (fn [db [_ character-id ability-id]]
   (assoc db :highlighted-ability {:character/id character-id
                                   :ability/id ability-id})))

(rf/reg-event-db
 :remove-highlighted-ability
 (fn [db _]
   (assoc db :highlighted-ability {:character/id nil
                                   :ability/id nil})))

(rf/reg-event-db
 :change-tab
 (fn [db [_ tab]]
   (assoc db :tab tab)))

(rf/reg-event-db
 :inc-ap-left
 (fn [db [_ char-uuid]]
   (update-in db [:characters char-uuid :ap-left] inc)))

(rf/reg-event-db
 :dec-ap-left
 (fn [db [_ char-uuid]]
   (update-in db [:characters char-uuid :ap-left] dec)))

(rf/reg-event-db
 :inc-health-left
 (fn [db [_ char-uuid]]
   (update-in db [:characters char-uuid :health-left] inc)))

(rf/reg-event-db
 :dec-health-left
 (fn [db [_ char-uuid]]
   (update-in db [:characters char-uuid :health-left] dec)))
