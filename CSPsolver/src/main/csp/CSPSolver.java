/**
 * @author Gage Messner
 */

package main.csp;

import java.time.LocalDate;
import java.util.*;

import javax.swing.SpringLayout.Constraints;

/**
 * CSP: Calendar Satisfaction Problem Solver
 * Provides a solution for scheduling some n meetings in a given
 * period of time and according to some unary and binary constraints
 * on the dates of each meeting.
 */
public class CSPSolver {

    // Backtracking CSP Solver
    // --------------------------------------------------------------------------------------------------------------
    
    /**
     * Public interface for the CSP solver in which the number of meetings,
     * range of allowable dates for each meeting, and constraints on meeting
     * times are specified.
     * @param nMeetings The number of meetings that must be scheduled, indexed from 0 to n-1
     * @param rangeStart The start date (inclusive) of the domains of each of the n meeting-variables
     * @param rangeEnd The end date (inclusive) of the domains of each of the n meeting-variables
     * @param constraints Date constraints on the meeting times (unary and binary for this assignment)
     * @return A list of dates that satisfies each of the constraints for each of the n meetings,
     *         indexed by the variable they satisfy, or null if no solution exists.
     */
    public static List<LocalDate> solve (int nMeetings, LocalDate rangeStart, LocalDate rangeEnd, Set<DateConstraint> constraints) {
    	List<LocalDate> assignment = new ArrayList<>();
    	List<MeetingDomain> domains = generateDomains(nMeetings, rangeStart, rangeEnd);
    	nodeConsistency(domains, constraints);
    	arcConsistency(domains, constraints);
    	return recursiveBT(assignment, nMeetings, constraints, domains);
    }
    
    /**
     * Recursively backtracks through our meeting domains and prunes inconsistent ones.
     * 
     * @param assignment A list of local dates which we assign meetings to
     * @param nMeetings The number of meetings that must be scheduled, indexed from 0 to n-1
     * @param dc Date constraints on the meeting times (unary and binary for this assignment)
     * @param domains A list of meeting domains
     * @return A list of local dates which are assigned meetings
     */
	private static List<LocalDate> recursiveBT(List<LocalDate> assignment, int nMeetings, Set<DateConstraint> dc, List<MeetingDomain> domains) {
		if (consistent(dc, assignment) && assignment.size() == nMeetings) {
			return assignment;
		}
		if (consistent(dc, assignment)) {
			for (LocalDate d : domains.get(assignment.size()).domainValues) {
				if (consistent(dc, assignment) && assignment.size() < domains.size()) {
					assignment.add(d);
					if (recursiveBT(assignment, nMeetings, dc, domains) != null) {
						assignment = recursiveBT(assignment, nMeetings, dc, domains);
						if (assignment.size() == nMeetings && consistent(dc, assignment)) {
							return assignment;
						}
					}
					assignment.remove(assignment.size()-1);
				}
			}
		}
		return null;
	}
    /**
     * Determines whether not our local dates are consistent with our constraints
     * @param constraints Date constraints on the meeting times (unary and binary for this assignment)
     * @param ld A list of local dates
     * @return boolean: true if our set of local dates is consistent with the given constraints
     */
	private static boolean consistent(Set<DateConstraint> constraints, List<LocalDate> ld) {
		for (DateConstraint dc : constraints) {
			if (dc.ARITY == 1) {
				UnaryDateConstraint uc = (UnaryDateConstraint) dc;
				if ((uc.L_VAL < ld.size())) {
					if (!dc.isSatisfiedBy(ld.get(dc.L_VAL), ((UnaryDateConstraint) dc).R_VAL)) {
						return false;
					}
				}
			} else {
				BinaryDateConstraint bc = (BinaryDateConstraint) dc;
				if (bc.R_VAL < ld.size() && bc.L_VAL < ld.size()) {
					if (!dc.isSatisfiedBy(ld.get(dc.L_VAL), ld.get(bc.R_VAL))) {
						return false;
					}
				}
			}
		}
		return true;
	}

    /**
     * Helper method for generating uniform domains for tests.
     * @param n Number of meeting variables in this CSP.
     * @param startRange Start date for the range of each variable's domain.
     * @param endRange End date for the range of each variable's domain.
     * @return The List of Meeting-indexed MeetingDomains.
     */
    private static List<MeetingDomain> generateDomains (int n, LocalDate startRange, LocalDate endRange) {
        List<MeetingDomain> domains = new ArrayList<>();
        while (n > 0) {
            domains.add(new MeetingDomain(startRange, endRange));
            n--;
        }
        return domains;
    }

    // Filtering Operations
    // --------------------------------------------------------------------------------------------------------------
    
    /**
     * Enforces node consistency for all variables' domains given in varDomains based on
     * the given constraints. Meetings' domains correspond to their index in the varDomains List.
     * @param varDomains List of MeetingDomains in which index i corresponds to D_i
     * @param constraints Set of DateConstraints specifying how the domains should be constrained.
     * [!] Note, these may be either unary or binary constraints, but this method should only process
     *     the *unary* constraints! 
     */
	public static void nodeConsistency(List<MeetingDomain> varDomains, Set<DateConstraint> constraints) {
		for (DateConstraint c : constraints) {
			if (c.ARITY == 2) {
				continue;
			} else {
				UnaryDateConstraint uc = (UnaryDateConstraint) c;
				MeetingDomain domain = varDomains.get(uc.L_VAL);
				Set<LocalDate> dom = domain.domainValues;
				Set<LocalDate> domainCopy = new HashSet<>(dom);
				for (LocalDate ld : dom) {
					if (!uc.isSatisfiedBy(ld, uc.R_VAL)) {
						domainCopy.remove(ld);
					}
				}
				domain.domainValues = domainCopy;
			}
		}
	}

    /**
     * Enforces arc consistency for all variables' domains given in varDomains based on
     * the given constraints. Meetings' domains correspond to their index in the varDomains List.
     * @param varDomains List of MeetingDomains in which index i corresponds to D_i
     * @param constraints Set of DateConstraints specifying how the domains should be constrained.
     * [!] Note, these may be either unary or binary constraints, but this method should only process
     *     the *binary* constraints using the AC-3 algorithm! 
     */
    public static void arcConsistency (List<MeetingDomain> varDomains, Set<DateConstraint> constraints) {
    	Set<Arc> arcSet = arcMaker(constraints);
    	Set<Arc> arcSetCopy = new HashSet<>(arcSet);
    	ac3(varDomains, constraints, arcSet, arcSetCopy);
    }
    
    
    /**
     * Arc-Consistency 3 Function we learned about
     * 
     * @param varDomains List of MeetingDomains in which index i corresponds to D_i
     * @param constraints Set of DateConstraints specifying how the domains should be constrained.
     * @param arcSet The arcs we are checking 
     * @param arcSetCopy A copy of our arc set
     */
    private static void ac3 (List<MeetingDomain> varDomains, Set<DateConstraint> constraints, Set<Arc> arcSet, Set<Arc> arcSetCopy) {
    	while(arcSet.size() != 0) {
    		Arc curArc = arcSet.iterator().next();
    		arcSet.remove(curArc);
    		if(pruner(varDomains, curArc)) {
    			for(Arc a : arcSetCopy) {
    				if(curArc.TAIL == a.HEAD) {
    					arcSet.add(a);
    				}
    			}
    		}
    	}
    }
    
     /**
     * Prunes inconsistent values for binary constraints.
     * 
     * @param md Our list of Meeting Domains
     * @param curArc The current arc we are examining
     * @return boolean: true if a value is pruned from the tail domain
     */
	private static boolean pruner(List<MeetingDomain> md, Arc curArc) {
		boolean removed = false;
		MeetingDomain tailDomain = md.get(curArc.TAIL);
		MeetingDomain headDomain = md.get(curArc.HEAD);
		Set<LocalDate> dateCopy = new HashSet<>(tailDomain.domainValues);
		for (LocalDate tailDate : tailDomain.domainValues) {
			boolean satisfied = false;
			for (LocalDate headDate : headDomain.domainValues) {
				if (curArc.CONSTRAINT.isSatisfiedBy(tailDate, headDate)) {
					satisfied = true;
				}
			}
			if(satisfied == false) {
				removed = true;
				dateCopy.remove(tailDate);
			}
		}
		tailDomain.domainValues = dateCopy;
		return removed;
	}
	
	/**
	 * Creates arcs and adds them into an set.
	 * 
     * @param dc Set of DateConstraints specifying how the domains should be constrained.
	 * @return a set of arcs
	 */
	private static Set<Arc> arcMaker(Set<DateConstraint> dc){
    	Set<Arc> arcSet = new HashSet<>();
    	for(DateConstraint c : dc) {
    		if(c.ARITY == 1) {
    			continue;
    		}
    		BinaryDateConstraint bc = (BinaryDateConstraint) c;
    		Arc forwardArc = new Arc(bc.L_VAL, bc.R_VAL , bc);
    		Arc backwardArc = new Arc(bc.R_VAL, bc.L_VAL , bc.getReverse());
   			arcSet.add(forwardArc);	
   			arcSet.add(backwardArc);
    	}
    	return arcSet;
	}
    
    /**
     * Private helper class organizing Arcs as defined by the AC-3 algorithm, useful for implementing the
     * arcConsistency method.
     * [!] You may modify this class however you'd like, its basis is just a suggestion that will indeed work.
     */
    private static class Arc {
        
        public final DateConstraint CONSTRAINT;
        public final int TAIL, HEAD;
        
        /**
         * Constructs a new Arc (tail -> head) where head and tail are the meeting indexes
         * corresponding with Meeting variables and their associated domains.
         * @param tail Meeting index of the tail
         * @param head Meeting index of the head
         * @param c Constraint represented by this Arc.
         * [!] WARNING: A DateConstraint's isSatisfiedBy method is parameterized as:
         * isSatisfiedBy (LocalDate leftDate, LocalDate rightDate), meaning L_VAL for the first
         * parameter and R_VAL for the second. Be careful with this when creating Arcs that reverse
         * direction. You may find the BinaryDateConstraint's getReverse method useful here.
         */
        public Arc (int tail, int head, DateConstraint c) {
            this.TAIL = tail;
            this.HEAD = head;
            this.CONSTRAINT = c;
        }
        
        @Override
        public boolean equals (Object other) {
            if (this == other) { return true; }
            if (this.getClass() != other.getClass()) { return false; }
            Arc otherArc = (Arc) other;
            return this.TAIL == otherArc.TAIL && this.HEAD == otherArc.HEAD && this.CONSTRAINT.equals(otherArc.CONSTRAINT);
        }
        
        @Override
        public int hashCode () {
            return Objects.hash(this.TAIL, this.HEAD, this.CONSTRAINT);
        }
        
        @Override
        public String toString () {
            return "(" + this.TAIL + " -> " + this.HEAD + ")";
        }
        
    }
    
}
