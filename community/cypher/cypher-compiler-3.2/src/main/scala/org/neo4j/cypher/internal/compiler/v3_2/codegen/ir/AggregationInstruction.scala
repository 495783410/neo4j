/*
 * Copyright (c) 2002-2016 "Neo Technology,"
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
package org.neo4j.cypher.internal.compiler.v3_2.codegen.ir

import org.neo4j.cypher.internal.compiler.v3_2.codegen.{Variable, CodeGenContext, MethodStructure}
import org.neo4j.cypher.internal.compiler.v3_2.codegen.ir.expressions.AggregateExpression

case class AggregationInstruction(opName: String, aggregationFunctions: Map[Variable, AggregateExpression])
  extends Instruction {

  override def init[E](generator: MethodStructure[E])(implicit context: CodeGenContext) {
    aggregationFunctions.values.foreach(e => e.init(generator))
    aggregationFunctions.foreach {
      case (v, e) => generator.assign(v.name, v.codeGenType, e.initialValue(generator))
    }
  }

  override def body[E](generator: MethodStructure[E])(implicit context: CodeGenContext) = {
    generator.trace(opName) { l1 =>
      aggregationFunctions.foreach {
        case (v, e) => l1.assign(v.name, v.codeGenType, e.generateExpression(l1))
      }
    }
  }

  override protected def children: Seq[Instruction] = Seq.empty

  override protected def operatorId = Set(opName)
}
