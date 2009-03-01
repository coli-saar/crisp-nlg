package crisp.converter;

import crisp.planningproblem.Domain;
import crisp.planningproblem.goal.Goal;
import crisp.planningproblem.Problem;
import crisp.planningproblem.Predicate;
import crisp.planningproblem.TypedVariableList;
import crisp.planningproblem.codec.PddlOutputCodec;


import de.saar.chorus.term.Compound;
import de.saar.chorus.term.Constant;
import de.saar.chorus.term.Substitution;
import de.saar.chorus.term.Term;
import de.saar.chorus.term.Variable;


import crisp.planningproblem.DurativeAction;

import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.File;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

public class CRISPtoTempPDDL {

    private static int maximumArity = 10;

	/**
	 * Main program.  When running the converter from the command line, pass
	 * the name of the CRISP problem file as the first argument.
	 *
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

        long start = System.currentTimeMillis();
        PrecomputedActions actions  = new PrecomputedActions(new File(args[0]));
        long end = System.currentTimeMillis();
        long precomputeTime = end-start;

        // Initialize new domain
        Domain domain = new Domain();
        setupDomain(domain);


		start = System.currentTimeMillis();
        Problem problem = ProblemParser.parseProblem(new File(args[1]),domain);
        String domainName = "domain-"+problem.getName();
        domain.setName(domainName);
        problem.setDomain(domainName);
        computeGoal(domain,problem);
		end = System.currentTimeMillis();
        long problemCreationTime = end-start;

		start = System.currentTimeMillis();
        ArrayList<DurativeAction> selectedActions = actions.getAllActions();
        //  Add all actions and corresponding predicates and constants to 
        //  the domain
        for (DurativeAction a: selectedActions){
            domain.addAction(a);
            HashMap<String,String> constants = a.getDomainConstants();
            if (constants!=null)
                for (String key : constants.keySet())
                    domain.addConstant(key,constants.get(key));

            ArrayList<Predicate> predicates = a.getDomainPredicates();
            if (predicates!=null)
                for (Predicate pred : predicates)
                    domain.addPredicate(pred);
             
        }
		end = System.currentTimeMillis();
        long domainCreationTime = end-start;

        

		System.out.println("Parse grammar and precompute actions: " + precomputeTime);
		System.out.println("Create Problem: " + problemCreationTime);
		System.out.println("Create domain: " + domainCreationTime);

        PrintWriter domainwriter = new PrintWriter(new FileWriter(new File(args[2])));
        PrintWriter problemwriter = new PrintWriter(new FileWriter(new File(args[3])));
		new PddlOutputCodec().writeToDisk(domain, problem, domainwriter, problemwriter);
	}



    /**
     * Compute the goal specification for the given CRISP problem.  This becomes the
     * "goal" clause in the PDDL problem.
     *
     * @param domain
     * @param problem
     */
    private static void computeGoal(Domain domain, Problem problem) {
        TypedVariableList tlNodeIndiv = new TypedVariableList();
        tlNodeIndiv.addItem(new Variable("?u"), "syntaxnode");
        tlNodeIndiv.addItem(new Variable("?x"), "individual");

        TypedVariableList tlCatNode = new TypedVariableList();
        tlCatNode.addItem(new Variable("?a"), "category");
        tlCatNode.addItem(new Variable("?u"), "syntaxnode");

        // collect all goals in this list
        List<Goal> finalStateGoals = new ArrayList<Goal>();

        // no positive "subst" literals in the goal state
        Goal noSubst = new crisp.planningproblem.goal.Universal(tlCatNode,
                new crisp.planningproblem.goal.Literal("subst(?a,?u)", false));

        // no positive "distractor" literals in the goal state
        Goal noDistractors = new crisp.planningproblem.goal.Universal(tlNodeIndiv,
                new crisp.planningproblem.goal.Literal("distractor(?u,?x)", false));

        // no positive "mustadjoin" literals in the goal state
        Goal noMustAdj= new crisp.planningproblem.goal.Universal(tlCatNode,
                new crisp.planningproblem.goal.Literal("mustadjoin(?a,?u)", false));

        finalStateGoals.add(noSubst);
        finalStateGoals.add(noDistractors);
        finalStateGoals.add(noMustAdj);

        // no positive needtoexpress-* literals, for any arity
        for( int i = 1; i <= maximumArity; i++ ) {
            TypedVariableList tlPredicate = new TypedVariableList();
            tlPredicate.addItem(new Variable("?P"), "predicate");

            Predicate predNTE = new Predicate();
            predNTE.setLabel("needtoexpress-" + i);
            predNTE.addVariable("?P", "predicate");

            List<Term> subterms = new ArrayList<Term>();
            subterms.add(new Variable("?P"));

            for( int j = 1; j <= i; j++ ) {
                tlPredicate.addItem(new Variable("?x" + j), "individual");
                subterms.add(new Variable("?x" + j));

                predNTE.addVariable("?x" + j, "individual");
            }

            finalStateGoals.add(new crisp.planningproblem.goal.Universal(tlPredicate,
                    new crisp.planningproblem.goal.Literal(new Compound("needtoexpress-" + i, subterms), false)));

            domain.addPredicate(predNTE);
        }


        problem.setGoal(new crisp.planningproblem.goal.Conjunction(finalStateGoals));
    }



        /**
     * Sets up the PDDL domain by registering the requirements, types, and a 
     * bunch of constants. This is called before parsing, therefore part of the
     * domain information cannot be set here (e.g. maximum plan length etc.)
     *
     * @param domain
     * @param problem
     */
    private static void setupDomain(Domain domain) {
        domain.clear();

        domain.addRequirement(":strips");
        domain.addRequirement(":equality");
        domain.addRequirement(":typing");
        domain.addRequirement(":conditional-effects");
        domain.addRequirement(":universal-preconditions");
        domain.addRequirement(":quantified-preconditions");
        domain.addRequirement(":durative-actions");

        domain.addSubtype("individual", "object");
        domain.addSubtype("category", "object");
        domain.addSubtype("syntaxnode", "object");
        domain.addSubtype("stepindex", "object");
        domain.addSubtype("predicate", "object");
        domain.addSubtype("rolename", "object");
        domain.addSubtype("treename", "object");

        Predicate predSubst = new Predicate(); predSubst.setLabel("subst");
        predSubst.addVariable("?t", "treename");
        predSubst.addVariable("?x", "category"); predSubst.addVariable("?y", "syntaxnode");
        domain.addPredicate(predSubst);

        Predicate predStep = new Predicate(); predStep.setLabel("step");
        predStep.addVariable("?i", "stepindex");
        domain.addPredicate(predStep);

        Predicate predDistractor = new Predicate(); predDistractor.setLabel("distractor");
        predDistractor.addVariable("?u", "syntaxnode"); predDistractor.addVariable("?x", "individual");
        domain.addPredicate(predDistractor);

        Predicate predReferent = new Predicate(); predReferent.setLabel("referent");
        predReferent.addVariable("?u", "syntaxnode"); predReferent.addVariable("?x", "individual");
        domain.addPredicate(predReferent);

        Predicate predCanadjoin = new Predicate(); predCanadjoin.setLabel("canadjoin");
        predCanadjoin.addVariable("?x", "category"); predCanadjoin.addVariable("?y", "syntaxnode");
        domain.addPredicate(predCanadjoin);

        Predicate predMustadjoin = new Predicate(); predMustadjoin.setLabel("mustadjoin");
        predMustadjoin.addVariable("?x", "category"); predMustadjoin.addVariable("?y", "syntaxnode");
        domain.addPredicate(predMustadjoin);
        
        domain.addConstant("S","category");
        domain.addConstant("init","syntaxnode");
        domain.addConstant("root","treename");
        
    }

}