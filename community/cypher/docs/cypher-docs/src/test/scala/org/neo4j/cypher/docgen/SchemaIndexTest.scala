/*
 * Copyright (c) 2002-2015 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.docgen

import org.hamcrest.CoreMatchers._
import org.junit.Assert._
import org.junit.Test
import org.neo4j.cypher.QueryStatisticsTestSupport
import org.neo4j.cypher.internal.compiler.v2_3.executionplan.InternalExecutionResult
import org.neo4j.cypher.internal.compiler.v2_3.pipes.IndexSeekByRange
import org.neo4j.cypher.internal.compiler.v2_3.planDescription.InternalPlanDescription.Arguments.Planner
import org.neo4j.cypher.internal.compiler.v2_3.{DPPlannerName, IDPPlannerName, GreedyPlannerName, RulePlannerName}
import org.neo4j.cypher.internal.helpers.GraphIcing

class SchemaIndexTest extends DocumentingTestBase with QueryStatisticsTestSupport with GraphIcing {

  //need a couple of 'Person' to make index operations more efficient than label scans
  override val setupQueries = (1 to 20 map (_ => """CREATE (:Person)""")).toList

  override def graphDescription = List(
    "andres:Person KNOWS mark:Person"
  )

  override val properties = Map(
    "andres" -> Map("name" -> "Andres"),
    "mark" -> Map("name" -> "Mark")
  )

  override val setupConstraintQueries = List(
    "CREATE INDEX ON :Person(name)"
  )

  def section = "Schema Index"

  @Test def create_index_on_a_label() {
    testQuery(
      title = "Create index on a label",
      text = "To create an index on a property for all nodes that have a label, use +CREATE+ +INDEX+ +ON+. " +
        "Note that the index is not immediately available, but will be created in the background. " +
        "See <<graphdb-neo4j-schema-indexes>> for details.",
      queryText = "create index on :Person(name)",
      optionalResultExplanation = "",
      assertions = (p) => assertIndexesOnLabels("Person", List(List("name")))
    )
  }

  @Test def drop_index_on_a_label() {
    prepareAndTestQuery(
      title = "Drop index on a label",
      text = "To drop an index on all nodes that have a label, use the +DROP+ +INDEX+ clause.",
      prepare = _ => executePreparationQueries(List("create index on :Person(name)")),
      queryText = "drop index on :Person(name)",
      optionalResultExplanation = "",
      assertions = (p) => assertIndexesOnLabels("Person", List())
    )
  }

  @Test def use_index() {
    testQuery(
      title = "Use index",
      text = "There is usually no need to specify which indexes to use in a query, Cypher will figure that out by itself. " +
        "For example the query below will use the `Person(name)` index, if it exists. " +
        "If you want Cypher to use specific indexes, you can enforce it using hints. See <<query-using>>.",
      queryText = "match (person:Person {name: 'Andres'}) return person",
      optionalResultExplanation = "",
      assertions = {
        (p) =>
          assertEquals(1, p.size)

          checkPlanDescription(p)("SchemaIndex", "NodeIndexSeek")
      }
    )
  }

  @Test def use_index_with_where_using_equality() {
    testQuery(
      title = "Use index with WHERE using equality",
      text = "Indexes are also automatically used for equality comparisons of an indexed property in the WHERE clause. " +
        "If you want Cypher to use specific indexes, you can enforce it using hints. See <<query-using>>.",
      queryText = "match (person:Person) WHERE person.name = 'Andres' return person",
      optionalResultExplanation = "",
      assertions = {
        (p) =>
          assertEquals(1, p.size)

          checkPlanDescription(p)("SchemaIndex", "NodeIndexSeek")
      }
    )
  }

  @Test def use_index_with_where_using_inequality() {
    // Need to make index preferable in terms of cost
    executePreparationQueries((0 to 300).map { i =>
      "CREATE (:Person)"
    }.toList)
    testQuery(
      title = "Use index with WHERE using inequality",
      text = "Indexes are also automatically used for inequality (range) comparisons of an indexed property in the WHERE clause. " +
        "If you for some reason want to hint to specific indexes, see <<query-using>>.",
      queryText = "match (person:Person) WHERE person.name > 'B' return person",
      optionalResultExplanation = "",
      assertions = {
        (p) =>
          assertEquals(1, p.size)

          checkPlanDescription(p)("SchemaIndex", "NodeIndexSeek")
      }
    )
  }

  @Test def use_index_with_in() {
    testQuery(
      title = "Use index with IN",
      text =
        "The IN predicate on `person.name` in the following query will use the `Person(name)` index, if it exists. " +
        "If you want Cypher to use specific indexes, you can enforce it using hints. See <<query-using>>.",
      queryText = "match (person:Person) WHERE person.name IN ['Andres','Mark'] return person",
      optionalResultExplanation = "",
      assertions = {
        (p) =>
          assertEquals(2, p.size)

          checkPlanDescription(p)("SchemaIndex", "NodeIndexSeek")
      }
    )
  }

  @Test def use_index_with_like() {
    // Need to make index preferable in terms of cost
    executePreparationQueries((0 to 250).map { i =>
      "CREATE (:Person)"
    }.toList)
    profileQuery(
      title = "Use index with LIKE",
      text =
        "The `LIKE` predicate on `person.name` in the following query will use the `Person(name)` index, if it exists. ",
      queryText = "MATCH (person:Person) WHERE person.name LIKE 'And%' return person",
      assertion = {
        (p) =>
          assertEquals(1, p.size)
          assertThat(p.executionPlanDescription().toString, containsString(IndexSeekByRange.name))
      }
    )
  }

  @Test def use_index_with_has_property() {
    // Need to make index preferable in terms of cost
    executePreparationQueries((0 to 250).map { i =>
      "CREATE (:Person)"
    }.toList)
    profileQuery(
      title = "Use index when checking for the existence of a property",
      text =
        "The `has(p.name)` predicate in the following query will use the `Person(name)` index, if it exists.",
      queryText = "MATCH (p:Person) WHERE has(p.name) RETURN p",
      assertion = {
        (p) =>
          assertEquals(2, p.size)
          assertThat(p.executionPlanDescription().toString, containsString("NodeIndexScan"))
      }
    )
  }

  def assertIndexesOnLabels(label: String, expectedIndexes: List[List[String]]) {
    assert(expectedIndexes === db.indexPropsForLabel(label))
  }

  private def checkPlanDescription(result: InternalExecutionResult)(ruleString: String, costString: String): Unit = {
    val planDescription = result.executionPlanDescription()
    val plannerArgument = planDescription.arguments.find(a => a.name == "planner")

    plannerArgument match {
      case Some(Planner(RulePlannerName.name)) =>
        assertThat(planDescription.toString, containsString(ruleString))
      case Some(Planner(GreedyPlannerName.name)) =>
        assertThat(planDescription.toString, containsString(costString))
      case Some(Planner(IDPPlannerName.name)) =>
        assertThat(planDescription.toString, containsString(costString))
      case Some(Planner(DPPlannerName.name)) =>
        assertThat(planDescription.toString, containsString(costString))

      case x =>
        fail(s"Couldn't determine used planner: $x")
    }
  }
}
