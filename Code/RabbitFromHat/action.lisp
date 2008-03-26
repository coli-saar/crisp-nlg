 (:action init-trans-imperative-take-P-from-1
      :parameters (?u - syntaxnode  ?x1 - individual  ?x2 - individual  ?x3 - individual ?s - partition  )
      :precondition (and (distractorset ?s in ?x2 ?x3) (step step1) (referent ?u ?x1) (subst S ?u) (takefrom ?x1 ?x2 ?x3))
      :effect (and (not (step step1)) 
		   (step step2) 
		   (not (subst S ?u)) 
		   (not (needtoexpress-3 takefrom ?x1 ?x2 ?x3))
		   (forall (?y - individual  ) (when (not (and (takefrom ?y ?x2 ?x3))) (not (distractor ?u ?y))))
		   (subst NP obj-1)
		   (referent obj-1 ?x2) 
		   (subst NP ppobj-1)
		   (referent ppobj-1 ?x3)
		   (canadjoin S ?u)
		   (canadjoin VP ?u) 
		   (canadjoin V ?u)
		   (canadjoin VP ?u)
		   (canadjoin V ?u)
		   (canadjoin PP ppobj-1)
		   (canadjoin P ppobj-1)
;		   (forall (?y - individual  ) (when (and (not (= ?y ?x2)) (in ?y ?x3)) (distractor obj-1 ?y))) 
;		   (forall (?y - individual  ) (when (and (not (= ?y ?x3)) (in ?x2 ?y)) (distractor ppobj-1 ?y))) 
		   (forall (?y - individual)
			   (when (distractorL ?s ?y) (distractor obj-1 ?y)))
		   (forall (?y - individual)
			   (when (distractorR ?s ?y) (distractor ppobj-1 ?y)))
	      )
   )
