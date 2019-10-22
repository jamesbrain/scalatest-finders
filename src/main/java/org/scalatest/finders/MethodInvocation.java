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

public class MethodInvocation implements AstNode {
  private final String className;
  private final AstNode target;
  private final AstNode parent;
  private final List<AstNode> children;
  private final String name;
  private final AstNode[] args;
  
  public MethodInvocation(String className, AstNode target, AstNode parent, AstNode[] children, String name, AstNode... args) {
    this.className = className;
    this.target = target;
    this.parent = parent;
    if (parent != null)
      parent.addChild(this);
    this.children = new ArrayList<>();
    this.children.addAll(Arrays.asList(children));
    this.name = name;
    this.args = args;
  }
  
  public String className() {
    return className;
  }
  
  public AstNode parent() {
    return parent;
  }
  
  public AstNode[] children() {
    return children.toArray(new AstNode[0]);
  }
  
  public String name() {
    return name;
  }
  
  public void addChild(AstNode node) {
    if (!children.contains(node))
      children.add(node);
  }

  @Override
  public boolean canBePartOfTestName() {
    for (AstNode child: children) {
      if (!child.canBePartOfTestName()) {
        return false;
      }
    }
    return true;
  }

  public AstNode target() {
    return target;
  }
  
  public AstNode[] args() {
    return args;  
  }
}

/*package org.scalatest.finders

class MethodInvocation (
  pClassName: String,
  pTarget: AstNode, 
  pParent: AstNode,
  pChildren: Array[AstNode],
  pName: String, 
  pArgs: AstNode*) 
extends AstNode {
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
  def name = pName
  def addChild(node: AstNode) = if (!childrenBuffer.contains(node)) childrenBuffer += node
  def target = pTarget
  def args = pArgs
}

object MethodInvocation {
  def apply(className: String, target: AstNode, parent: AstNode, children: Array[AstNode], name: String, args: AstNode*): MethodInvocation = 
    new MethodInvocation(className, target, parent, children, name, args.toList: _*)
  def unapply(value: MethodInvocation): Option[(String, AstNode, AstNode, Array[AstNode], String, Array[AstNode])] = 
    if (value != null)
      Some((value.className, value.target, value.parent, value.children, value.name, value.args.toArray))
    else
      None
}*/