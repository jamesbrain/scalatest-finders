/*
 * Copyright 2001-2008 Artima, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.scalatest.finders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ToStringTarget implements AstNode {
    
  private final String className;
  private final AstNode parent;
  private final List<AstNode> children;
  private final Object target;
    
  public ToStringTarget(String className, AstNode parent, AstNode[] children, Object target) {
    this.className = className;
    this.parent = parent;
    if (parent != null)
        parent.addChild(this);
    this.children = new ArrayList<>();
    this.children.addAll(Arrays.asList(children));
    this.target = target;
  }
  
  public String className() {
    return className;
  }
  
  public AstNode parent() {
    return parent;
  }
  
  public AstNode[] children() {
    return new AstNode[0];
  }
  
  public String name() {
    return target.toString();
  }
    
  public void addChild(AstNode node) {
    if (!children.contains(node))
      children.add(node);
  }

  @Override
  public boolean canBePartOfTestName() {
    return true;
  }

  @Override
  public String toString() {
    return target.toString();
  }
}

/*package org.scalatest.finders

class ToStringTarget(pClassName: String, pParent: AstNode, pChildren: Array[AstNode], val target: AnyRef) extends AstNode {
  import scala.collection.mutable.ListBuffer
  private val childrenBuffer = new ListBuffer[AstNode]()
  childrenBuffer ++= pChildren
  
  def className = pClassName
  lazy val parent = getParent
  protected def getParent() = {
    if (pParent != null)
      pParent.addChild(this)
    pParent
  }
  def children = childrenBuffer.toArray
  def name = target.toString
  def addChild(node: AstNode) = if (!childrenBuffer.contains(node)) childrenBuffer += node
  override def toString() = {
    target.toString
  }
}

object ToStringTarget {
  def apply(className: String, parent: AstNode, children: Array[AstNode], target: AnyRef) = 
    new ToStringTarget(className, parent, children, target)
  def unapply(value: ToStringTarget): Option[(String, AstNode, Array[AstNode], AnyRef)] = 
    if (value != null)
      Some((value.className, value.parent, value.children, value.target))
    else
      None
}*/