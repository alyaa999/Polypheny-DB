/*
 * Copyright 2019-2023 The Polypheny Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.polypheny.db.sql;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.polypheny.db.algebra.AbstractAlgNode;
import org.polypheny.db.algebra.AlgNode;
import org.polypheny.db.algebra.AlgRoot;
import org.polypheny.db.algebra.core.AlgFactories;
import org.polypheny.db.algebra.logical.relational.LogicalRelIntersect;
import org.polypheny.db.algebra.logical.relational.LogicalRelUnion;
import org.polypheny.db.algebra.rules.CalcMergeRule;
import org.polypheny.db.algebra.rules.CoerceInputsRule;
import org.polypheny.db.algebra.rules.FilterToCalcRule;
import org.polypheny.db.algebra.rules.ProjectRemoveRule;
import org.polypheny.db.algebra.rules.ProjectToCalcRule;
import org.polypheny.db.algebra.rules.ReduceExpressionsRules;
import org.polypheny.db.algebra.rules.UnionToDistinctRule;
import org.polypheny.db.plan.AlgOptListener;
import org.polypheny.db.plan.hep.HepMatchOrder;
import org.polypheny.db.plan.hep.HepPlanner;
import org.polypheny.db.plan.hep.HepProgram;
import org.polypheny.db.plan.hep.HepProgramBuilder;


/**
 * HepPlannerTest is a unit test for {@link HepPlanner}. See {#@link RelOptRulesTest} for an explanation of how to add tests; the tests in this class are targeted at exercising the planner, and use specific rules for convenience only,
 * whereas the tests in that class are targeted at exercising specific rules, and use the planner for convenience only. Hence the split.
 */
@Disabled // this test heavily relies on unmaintainable string results and arcane naming conventions of tests, as well as trickery with Throwable.getStackTrace() to get the test name, which should be avoided and replaced with proper testing
public class HepPlannerTest extends AlgOptTestBase {

    private static final String UNION_TREE = "(select name from dept union select ename from emp)" + " union (select ename from bonus)";

    private static final String COMPLEX_UNION_TREE = """
            select * from (
              select ENAME, 50011895 as cat_id, '1' as cat_name, 1 as require_free_postage, 0 as require_15return, 0 as require_48hour,1 as require_insurance from emp where EMPNO = 20171216 and MGR = 0 and ENAME = 'Y' and SAL = 50011895 union all
              select ENAME, 50013023 as cat_id, '2' as cat_name, 0 as require_free_postage, 0 as require_15return, 0 as require_48hour,0 as require_insurance from emp where EMPNO = 20171216 and MGR = 0 and ENAME = 'Y' and SAL = 50013023 union all
              select ENAME, 50013032 as cat_id, '3' as cat_name, 0 as require_free_postage, 0 as require_15return, 0 as require_48hour,0 as require_insurance from emp where EMPNO = 20171216 and MGR = 0 and ENAME = 'Y' and SAL = 50013032 union all
              select ENAME, 50013024 as cat_id, '4' as cat_name, 0 as require_free_postage, 0 as require_15return, 0 as require_48hour,0 as require_insurance from emp where EMPNO = 20171216 and MGR = 0 and ENAME = 'Y' and SAL = 50013024 union all
              select ENAME, 50004204 as cat_id, '5' as cat_name, 0 as require_free_postage, 0 as require_15return, 0 as require_48hour,0 as require_insurance from emp where EMPNO = 20171216 and MGR = 0 and ENAME = 'Y' and SAL = 50004204 union all
              select ENAME, 50013043 as cat_id, '6' as cat_name, 0 as require_free_postage, 0 as require_15return, 0 as require_48hour,0 as require_insurance from emp where EMPNO = 20171216 and MGR = 0 and ENAME = 'Y' and SAL = 50013043 union all
              select ENAME, 290903 as cat_id, '7' as cat_name, 1 as require_free_postage, 0 as require_15return, 0 as require_48hour,1 as require_insurance from emp where EMPNO = 20171216 and MGR = 0 and ENAME = 'Y' and SAL = 290903 union all
              select ENAME, 50008261 as cat_id, '8' as cat_name, 1 as require_free_postage, 0 as require_15return, 0 as require_48hour,1 as require_insurance from emp where EMPNO = 20171216 and MGR = 0 and ENAME = 'Y' and SAL = 50008261 union all
              select ENAME, 124478013 as cat_id, '9' as cat_name, 0 as require_free_postage, 0 as require_15return, 1 as require_48hour,0 as require_insurance from emp where EMPNO = 20171216 and MGR = 0 and ENAME = 'Y' and SAL = 124478013 union all
              select ENAME, 124472005 as cat_id, '10' as cat_name, 0 as require_free_postage, 0 as require_15return, 1 as require_48hour,0 as require_insurance from emp where EMPNO = 20171216 and MGR = 0 and ENAME = 'Y' and SAL = 124472005 union all
              select ENAME, 50013475 as cat_id, '11' as cat_name, 0 as require_free_postage, 1 as require_15return, 1 as require_48hour,0 as require_insurance from emp where EMPNO = 20171216 and MGR = 0 and ENAME = 'Y' and SAL = 50013475 union all
              select ENAME, 50018263 as cat_id, '12' as cat_name, 0 as require_free_postage, 1 as require_15return, 1 as require_48hour,0 as require_insurance from emp where EMPNO = 20171216 and MGR = 0 and ENAME = 'Y' and SAL = 50018263 union all
              select ENAME, 50013498 as cat_id, '13' as cat_name, 0 as require_free_postage, 1 as require_15return, 1 as require_48hour,0 as require_insurance from emp where EMPNO = 20171216 and MGR = 0 and ENAME = 'Y' and SAL = 50013498 union all
              select ENAME, 350511 as cat_id, '14' as cat_name, 0 as require_free_postage, 1 as require_15return, 1 as require_48hour,0 as require_insurance from emp where EMPNO = 20171216 and MGR = 0 and ENAME = 'Y' and SAL = 350511 union all
              select ENAME, 50019790 as cat_id, '15' as cat_name, 0 as require_free_postage, 1 as require_15return, 1 as require_48hour,0 as require_insurance from emp where EMPNO = 20171216 and MGR = 0 and ENAME = 'Y' and SAL = 50019790 union all
              select ENAME, 50015382 as cat_id, '16' as cat_name, 0 as require_free_postage, 1 as require_15return, 1 as require_48hour,0 as require_insurance from emp where EMPNO = 20171216 and MGR = 0 and ENAME = 'Y' and SAL = 50015382 union all
              select ENAME, 350503 as cat_id, '17' as cat_name, 0 as require_free_postage, 1 as require_15return, 1 as require_48hour,0 as require_insurance from emp where EMPNO = 20171216 and MGR = 0 and ENAME = 'Y' and SAL = 350503 union all
              select ENAME, 350401 as cat_id, '18' as cat_name, 0 as require_free_postage, 1 as require_15return, 1 as require_48hour,0 as require_insurance from emp where EMPNO = 20171216 and MGR = 0 and ENAME = 'Y' and SAL = 350401 union all
              select ENAME, 50015560 as cat_id, '19' as cat_name, 0 as require_free_postage, 0 as require_15return, 0 as require_48hour,0 as require_insurance from emp where EMPNO = 20171216 and MGR = 0 and ENAME = 'Y' and SAL = 50015560 union all
              select ENAME, 122658003 as cat_id, '20' as cat_name, 0 as require_free_postage, 1 as require_15return, 1 as require_48hour,0 as require_insurance from emp where EMPNO = 20171216 and MGR = 0 and ENAME = 'Y' and SAL = 122658003 union all
              select ENAME, 50022371 as cat_id, '100' as cat_name, 0 as require_free_postage, 0 as require_15return, 0 as require_48hour,0 as require_insurance from emp where EMPNO = 20171216 and MGR = 0 and ENAME = 'Y' and SAL = 50022371
            ) a""";


    @Override
    protected DiffRepository getDiffRepos() {
        return DiffRepository.lookup( HepPlannerTest.class );
    }


    @Test
    public void testRuleClass() {
        // Verify that an entire class of rules can be applied.

        HepProgramBuilder programBuilder = HepProgram.builder();
        programBuilder.addRuleClass( CoerceInputsRule.class );

        HepPlanner planner = new HepPlanner( programBuilder.build() );

        planner.addRule( new CoerceInputsRule( LogicalRelUnion.class, false, AlgFactories.LOGICAL_BUILDER ) );
        planner.addRule( new CoerceInputsRule( LogicalRelIntersect.class, false, AlgFactories.LOGICAL_BUILDER ) );

        checkPlanning( planner, "(select name from dept union select ename from emp) intersect (select fname from customer.contact)" );
    }


    @Test
    public void testRuleDescription() {
        // Verify that a rule can be applied via its description.

        HepProgramBuilder programBuilder = HepProgram.builder();
        programBuilder.addRuleByDescription( "FilterToCalcRule" );

        HepPlanner planner = new HepPlanner( programBuilder.build() );

        planner.addRule( FilterToCalcRule.INSTANCE );

        checkPlanning( planner, "select name from sales.dept where deptno=12" );
    }


    /**
     * Ensures {@link AbstractAlgNode} digest does not include full digest tree.
     */
    @Test
    public void algDigestLength() {
        HepProgramBuilder programBuilder = HepProgram.builder();
        HepPlanner planner = new HepPlanner( programBuilder.build() );
        final int n = 10;
        String sb = "select * from ("
                + "select name from sales.dept"
                + " union all select name from sales.dept".repeat( n )
                + ")";
        AlgRoot root = tester.convertSqlToAlg( sb );
        planner.setRoot( root.alg );
        AlgNode best = planner.findBestExp();

        // Good digest should look like rel#66:LogicalProject(input=rel#64:LogicalUnion)
        // Bad digest includes full tree like rel#66:LogicalProject(input=rel#64:LogicalUnion(...))
        // So the assertion is to ensure digest includes LogicalUnion exactly once

        assertIncludesExactlyOnce( "best.getDescription()", best.getDescription(), "LogicalUnion" );
        assertIncludesExactlyOnce( "best.getDigest()", best.getDigest(), "LogicalUnion" );
    }


    private void assertIncludesExactlyOnce( String message, String digest, String substring ) {
        int pos = 0;
        int cnt = 0;
        while ( pos >= 0 ) {
            pos = digest.indexOf( substring, pos + 1 );
            if ( pos > 0 ) {
                cnt++;
            }
        }
        assertEquals( 1, cnt, message + " should include <<" + substring + ">> exactly once, actual value is " + digest );
    }


    @Test
    public void testMatchLimitOneTopDown() {
        // Verify that only the top union gets rewritten.

        HepProgramBuilder programBuilder = HepProgram.builder();
        programBuilder.addMatchOrder( HepMatchOrder.TOP_DOWN );
        programBuilder.addMatchLimit( 1 );
        programBuilder.addRuleInstance( UnionToDistinctRule.INSTANCE );

        checkPlanning( programBuilder.build(), UNION_TREE );
    }


    @Test
    public void testMatchLimitOneBottomUp() {
        // Verify that only the bottom union gets rewritten.

        HepProgramBuilder programBuilder = HepProgram.builder();
        programBuilder.addMatchLimit( 1 );
        programBuilder.addMatchOrder( HepMatchOrder.BOTTOM_UP );
        programBuilder.addRuleInstance( UnionToDistinctRule.INSTANCE );

        checkPlanning( programBuilder.build(), UNION_TREE );
    }


    @Test
    public void testMatchUntilFixpoint() {
        // Verify that both unions get rewritten.

        HepProgramBuilder programBuilder = HepProgram.builder();
        programBuilder.addMatchLimit( HepProgram.MATCH_UNTIL_FIXPOINT );
        programBuilder.addRuleInstance( UnionToDistinctRule.INSTANCE );

        checkPlanning( programBuilder.build(), UNION_TREE );
    }


    @Test
    @Disabled
    public void testReplaceCommonSubexpression() {
        // Note that here it may look like the rule is firing twice, but actually it's only firing once on the common sub-expression.  The purpose of this test
        // is to make sure the planner can deal with rewriting something used as a common sub-expression twice by the same parent (the join in this case).

        checkPlanning( ProjectRemoveRule.INSTANCE, "select d1.deptno from (select * from dept) d1, (select * from dept) d2" );
    }


    /**
     * Tests that if two algebra expressions are equivalent, the planner notices, and only applies the rule once.
     */
    @Test
    public void testCommonSubExpression() {
        // In the following,
        // (select 1 from dept where abs(-1)=20)
        // occurs twice, but it's a common sub-expression, so the rule should only apply once.
        HepProgramBuilder programBuilder = HepProgram.builder();
        programBuilder.addRuleInstance( FilterToCalcRule.INSTANCE );

        final HepTestListener listener = new HepTestListener( 0 );
        HepPlanner planner = new HepPlanner( programBuilder.build() );
        planner.addListener( listener );

        final String sql = """
                (select 1 from dept where abs(-1)=20)
                union all
                (select 1 from dept where abs(-1)=20)""";
        planner.setRoot( tester.convertSqlToAlg( sql ).alg );
        AlgNode bestAlg = planner.findBestExp();

        assertThat( bestAlg.getInput( 0 ).equals( bestAlg.getInput( 1 ) ), is( true ) );
        assertThat( listener.getApplyTimes() == 1, is( true ) );
    }


    @Test
    public void testSubprogram() {
        // Verify that subprogram gets re-executed until fixpoint. In this case, the first time through we limit it to generate only one calc; the second time through it will generate
        // a second calc, and then merge them.
        HepProgramBuilder subprogramBuilder = HepProgram.builder();
        subprogramBuilder.addMatchOrder( HepMatchOrder.TOP_DOWN );
        subprogramBuilder.addMatchLimit( 1 );
        subprogramBuilder.addRuleInstance( ProjectToCalcRule.INSTANCE );
        subprogramBuilder.addRuleInstance( CalcMergeRule.INSTANCE );

        HepProgramBuilder programBuilder = HepProgram.builder();
        programBuilder.addSubprogram( subprogramBuilder.build() );

        checkPlanning( programBuilder.build(), "select upper(ename) from (select lower(ename) as ename from emp)" );
    }


    @Test
    public void testGroup() throws Exception {
        // Verify simultaneous application of a group of rules. Intentionally add them in the wrong order to make sure that order doesn't matter within the group.
        HepProgramBuilder programBuilder = HepProgram.builder();
        programBuilder.addGroupBegin();
        programBuilder.addRuleInstance( CalcMergeRule.INSTANCE );
        programBuilder.addRuleInstance( ProjectToCalcRule.INSTANCE );
        programBuilder.addRuleInstance( FilterToCalcRule.INSTANCE );
        programBuilder.addGroupEnd();

        checkPlanning( programBuilder.build(), "select upper(name) from dept where deptno=20" );
    }


    @Test
    public void testGC() {
        HepProgramBuilder programBuilder = HepProgram.builder();
        programBuilder.addMatchOrder( HepMatchOrder.TOP_DOWN );
        programBuilder.addRuleInstance( CalcMergeRule.INSTANCE );
        programBuilder.addRuleInstance( ProjectToCalcRule.INSTANCE );
        programBuilder.addRuleInstance( FilterToCalcRule.INSTANCE );

        HepPlanner planner = new HepPlanner( programBuilder.build() );
        planner.setRoot( tester.convertSqlToAlg( "select upper(name) from dept where deptno=20" ).alg );
        planner.findBestExp();
        // Reuse of HepPlanner (should trigger GC).
        planner.setRoot( tester.convertSqlToAlg( "select upper(name) from dept where deptno=20" ).alg );
        planner.findBestExp();
    }


    @Test
    public void testRuleApplyCount() {
        final long applyTimes1 = checkRuleApplyCount( HepMatchOrder.ARBITRARY );
        assertThat( applyTimes1, is( 316L ) );

        final long applyTimes2 = checkRuleApplyCount( HepMatchOrder.DEPTH_FIRST );
        assertThat( applyTimes2, is( 87L ) );
    }


    private long checkRuleApplyCount( HepMatchOrder matchOrder ) {
        final HepProgramBuilder programBuilder = HepProgram.builder();
        programBuilder.addMatchOrder( matchOrder );
        programBuilder.addRuleInstance( ReduceExpressionsRules.FILTER_INSTANCE );
        programBuilder.addRuleInstance( ReduceExpressionsRules.PROJECT_INSTANCE );

        final HepTestListener listener = new HepTestListener( 0 );
        HepPlanner planner = new HepPlanner( programBuilder.build() );
        planner.addListener( listener );
        planner.setRoot( tester.convertSqlToAlg( COMPLEX_UNION_TREE ).alg );
        planner.findBestExp();
        return listener.getApplyTimes();
    }


    /**
     * Listener for HepPlannerTest; counts how many times rules fire.
     */
    private static class HepTestListener implements AlgOptListener {

        private long applyTimes;


        HepTestListener( long applyTimes ) {
            this.applyTimes = applyTimes;
        }


        long getApplyTimes() {
            return applyTimes;
        }


        @Override
        public void algEquivalenceFound( AlgEquivalenceEvent event ) {
        }


        @Override
        public void ruleAttempted( RuleAttemptedEvent event ) {
            if ( event.isBefore() ) {
                ++applyTimes;
            }
        }


        @Override
        public void ruleProductionSucceeded( RuleProductionEvent event ) {
        }


        @Override
        public void algDiscarded( AlgDiscardedEvent event ) {
        }


        @Override
        public void algChosen( AlgChosenEvent event ) {
        }

    }

}

