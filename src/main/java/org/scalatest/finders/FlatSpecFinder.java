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

import java.util.*;

import static org.scalatest.finders.LocationUtils.getParentOfType;
import static org.scalatest.finders.LocationUtils.isValidName;

public class FlatSpecFinder implements Finder {
  
  public Selection find(AstNode node) {
    Selection result = null;  
    while (result == null) {
      if (node instanceof ConstructorBlock) 
        result = getAllTestSelection(node.className(), node.children());
      else if (node instanceof MethodInvocation) {
        MethodInvocation invocation = (MethodInvocation) node;
        if (invocation.name().equals("of") || invocation.name().equals("in") || invocation.name().equals("should") || invocation.name().equals("must")) {
          ConstructorBlock constructor = getParentOfType(node, ConstructorBlock.class);
          if (constructor != null) {
            AstNode scopeNode = getScopeNode(node, constructor.children());
            if (scopeNode != null) {
              String prefix = getPrefix((MethodInvocation) scopeNode);
              result = getNodeTestSelection(node, prefix, constructor.children());
            }
          }
        }
      }
      
      if (result == null) {
        if (node.parent() != null) 
          node = node.parent();
        else
          break;
      }
    }
    return result;
  }
    
  private Selection getAllTestSelection(String className, AstNode[] constructorChildren) {
    String prefix = null;
    List<String> testNames = new ArrayList<>();
    for (AstNode child : constructorChildren) {
      if (isScope(child))
        prefix = getPrefix((MethodInvocation) child);
      if(prefix != null && child instanceof MethodInvocation && child.name().equals("in")) {
        String testName = getTestName(prefix, (MethodInvocation) child);
        if (testName != null) {
          testNames.add(testName);
        }
      }
    }
    return new Selection(className, className, testNames.toArray(new String[0]));
  }
    
  private String getPrefix(MethodInvocation invocation) {
    String result = null;
    while (result == null) {
      if (invocation.name().equals("of"))
        //result = invocation.target().toString();
        if (invocation.args()[0].canBePartOfTestName()) {
          result = invocation.args()[0].toString();
        } else return null;
      else {
        if (invocation.target() instanceof MethodInvocation) {
          MethodInvocation invocationTarget = (MethodInvocation) invocation.target();
          if (invocationTarget.name().equals("should") || invocationTarget.name().equals("must") ||
                  invocationTarget.name().equals("taggedAs"))
            invocation = invocationTarget;
          else if (invocation.target().canBePartOfTestName()) {
            result = invocation.target().toString();
          } else return null;
        }
        else if (invocation.target().canBePartOfTestName()) {
          result = invocation.target().toString();
        } else {
          return null;
        }
      }
    }
    return result;
  }
    
  private AstNode getScopeNode(AstNode node, AstNode[] constructorChildren) {
    AstNode topLevelNode = null;
    while (node != null && topLevelNode == null) {
      if (node.parent() instanceof ConstructorBlock)
       topLevelNode = node;
      else 
       node = node.parent();
    }
    
    if (topLevelNode != null) {
      if (isScope(topLevelNode))
        return topLevelNode;
      else {
        List<AstNode> beforeTopLevelNodeList = new ArrayList<>();
        for (AstNode child : constructorChildren) {
          if (!child.equals(topLevelNode)) 
            beforeTopLevelNodeList.add(child);
          else
            break;
        }
        AstNode scopeNode = null;
        for(int i=beforeTopLevelNodeList.size() - 1; i >= 0; i--) {
          AstNode tnode = beforeTopLevelNodeList.get(i);
          if (isScope(tnode)) {
            scopeNode = tnode;
            break;
          }
        }
        if (scopeNode == null) {
          AstNode tNode = beforeTopLevelNodeList.isEmpty() ? topLevelNode : beforeTopLevelNodeList.get(0);
          if (isScope(node, true)) {
            scopeNode = tNode;
          }
        }
        return scopeNode;
      }
    }
    else
      return null;
  }

  private boolean isScope(AstNode node) {
    return isScope(node, false);
  }

  private boolean isScope(AstNode node, boolean allowIt) {
    if (node instanceof MethodInvocation) {
      MethodInvocation invocation = (MethodInvocation) node;
      return invocation.name().equals("of") ||
              isScopeTagged(invocation, allowIt) ||
              (invocation.name().equals("in") && invocation.target() != null &&
                      invocation.target() instanceof MethodInvocation &&
                      isScopeTagged((MethodInvocation) invocation.target(), allowIt));
    }
    else
      return false;
  }

  private boolean isScopeTagged(MethodInvocation invocation, boolean allowIt) {
    return (invocation.name().equals("taggedAs") && invocation.target() instanceof MethodInvocation &&
            isScopeShould((MethodInvocation) invocation.target(), allowIt)) || isScopeShould(invocation, allowIt);
  }

  private boolean isScopeShould(MethodInvocation invocation, boolean allowIt) {
    return (invocation.name().equals("should") || invocation.name().equals("must")) && invocation.args().length > 0 &&
            invocation.target() != null && (allowIt || !isItOrThey(invocation));
  }

  private boolean isItOrThey(MethodInvocation invocation) {
    String name = invocation.target().name();
    return name.equals("it") || name.equals("they");
  }
    
  private Selection getNodeTestSelection(AstNode node, String prefix, AstNode[] constructorChildren) {
    if (prefix == null) {
      return null;
    }
    if (node instanceof ConstructorBlock) {
      List<String> testNames = getTestNamesFromChildren(prefix, Arrays.asList(node.children()));
      return new Selection(node.className(), prefix.length() > 0 ? prefix : node.className(), testNames.toArray(new String[0]));
    }
    else if (node instanceof MethodInvocation) {
      MethodInvocation invocation = (MethodInvocation) node;
      String name = invocation.name();
      switch (name) {
        case "of":
          List<AstNode> constructorChildrenList = Arrays.asList(constructorChildren);
          int nodeIdx = constructorChildrenList.indexOf(node);
          if (nodeIdx >= 0) {
            List<AstNode> startList = constructorChildrenList.subList(nodeIdx + 1, constructorChildrenList.size());
            List<AstNode> subList = new ArrayList<>();
            for (AstNode snode : startList) {
              if (!isScope(snode))
                subList.add(snode);
              else
                break;
            }
            List<String> testNames = getTestNamesFromChildren(prefix, subList);
            return new Selection(node.className(), prefix, testNames.toArray(new String[0]));
          } else
            return null;
        case "should":
        case "must":
          AstNode parent = invocation.parent();
          if (parent instanceof MethodInvocation && parent.name().equals("in")) {
            String testName = getTestName(prefix, (MethodInvocation) parent);
            return testName != null ? new Selection(invocation.className(), testName, new String[]{testName}) : null;
          } else
            return null;
        case "in":
          String testName = getTestName(prefix, invocation);
          return testName != null ? new Selection(invocation.className(), testName, new String[]{testName}) : null;
        default:
          return null;
      }
    }
    else 
      return null;
  }
    
  private List<String> getTestNamesFromChildren(String prefix, List<AstNode> children) {
    Set<String> validSet = new HashSet<>();
    validSet.add("in");
    List<String> testNameList = new ArrayList<>();
    for (AstNode node : children) {
      if (node instanceof MethodInvocation && isValidName(node.name(), validSet)) {
        MethodInvocation invocation = (MethodInvocation) node;
        String testName = getTestName(prefix, invocation);
        if (testName != null) {
          testNameList.add(testName);
        }
      }
    }
    return testNameList;
  }
    
  private String getTargetString(AstNode target, String prefix, String postfix)  {
    if (target == null)
      return postfix;
    else {
      if (target instanceof MethodInvocation && target.name().equals("should") && ((MethodInvocation) target).args()[0].canBePartOfTestName())
        return "should " + ((MethodInvocation) target).args()[0];
      else if (target instanceof MethodInvocation && target.name().equals("must") && ((MethodInvocation) target).args()[0].canBePartOfTestName())
        return "must " + ((MethodInvocation) target).args()[0];
      else if (target instanceof MethodInvocation && target.name().equals("taggedAs")) {
        return getTargetString(((MethodInvocation) target).target(), prefix, postfix);
      } else if (target.canBePartOfTestName())
        return target.toString();
      else return null;
    } 
  }
    
  private String getTestName(String prefix, MethodInvocation invocation) {
    String name = getTargetString(invocation.target(), prefix, "");
    return prefix == null || name == null ? null : prefix + " " + name;
  }
}

/*package org.scalatest.finders

import LocationUtils._
import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer

class FlatSpecFinder extends Finder {

  def find(node: AstNode): Option[Selection] = {
    node match {
      case constructor: ConstructorBlock => 
        getAllTestSelection(node.className, constructor.children)
      case invocation: MethodInvocation
        if invocation.name == "of" || invocation.name == "in" || invocation.name == "should" => 
        val constructorOpt: Option[ConstructorBlock] = node match {
          case constructor: ConstructorBlock => Some(constructor)
          case _ => 
            getParentOfType(node, classOf[ConstructorBlock])
        }
        constructorOpt match {
          case Some(constructor) =>
            val scopeNodeOpt = getScopeNode(node, constructor.children)
            scopeNodeOpt match {
              case Some(scopeNode) => 
                val prefix = getPrefix(scopeNode.asInstanceOf[MethodInvocation])
                getNodeTestSelection(node, prefix, constructor.children)
              case None => 
                if (node.parent != null)
                  find(node.parent)
                else
                  None
            }
          case None => None
        }
      case _ => 
        if (node.parent != null)
          find(node.parent)
        else
          None
    }
  }
  
  private def getAllTestSelection(className: String, constructorChildren: Array[AstNode]) = {
    var prefix: String = null
    val listBuffer = new ListBuffer[String]()
    for (child <- constructorChildren) {
      if (isScope(child))
        prefix = getPrefix(child.asInstanceOf[MethodInvocation])
      if(prefix != null && child.isInstanceOf[MethodInvocation] && child.name == "in") 
        listBuffer += getTestName(prefix, child.asInstanceOf[MethodInvocation])
    }
    Some(new Selection(className, className, listBuffer.toArray))
  }
  
  @tailrec
  private def getPrefix(scopeInvocation: MethodInvocation): String = {
    if (scopeInvocation.name == "of")
      scopeInvocation.args(0).toString
    else { 
      scopeInvocation.target match {
        case inInvocation @ MethodInvocation(className, target, parent, children, "should", args) => // in
          getPrefix(inInvocation)
        case _ => 
          scopeInvocation.target.toString
      }
    }
  }
  
  private def getScopeNode(node: AstNode, constructorChildren: Array[AstNode]): Option[AstNode] = {
    @tailrec
    def getTopLevelNode(node: AstNode): AstNode = 
      if (node.parent.isInstanceOf[ConstructorBlock])
        node
      else
        getTopLevelNode(node.parent)
    
    val topLevelNode = getTopLevelNode(node)
    if (isScope(topLevelNode))
      return Some(topLevelNode)
    else 
      constructorChildren.takeWhile(_ != topLevelNode).reverse.find(isScope(_))
  }
  
  private def isScope(node: AstNode): Boolean = {
    def isScopeShould(invocation: MethodInvocation) = invocation.name == "should" && invocation.args.length > 0 && invocation.target != null && invocation.target.toString != "it"
    node match {
      case invocation: MethodInvocation //(className, target, parent, children, name, args) 
        if invocation.name == "of" || 
           isScopeShould(invocation) || 
           (invocation.name == "in" && invocation.target != null && invocation.target.isInstanceOf[MethodInvocation] && isScopeShould(invocation.target.asInstanceOf[MethodInvocation]))
           => 
           true
      case _ =>
        false
    }
  }
  
  private def getNodeTestSelection(node: AstNode, prefix: String, constructorChildren: Array[AstNode]) = {
    node match {
      case ConstructorBlock(className, children) => 
        val testNames = getTestNamesFromChildren(prefix, children)
        Some(new Selection(className, if (prefix.length > 0) prefix else className, testNames))
      case invocation @ MethodInvocation(className, target, parent, children, name, args) =>
        if (name == "of") {
          val nodeIdx = constructorChildren.indexOf(node)
          if (nodeIdx >= 0) {
            val startList = constructorChildren.drop(nodeIdx + 1)
            val subList = startList.takeWhile(!isScope(_))
            val testNames = getTestNamesFromChildren(prefix, subList)
            Some(new Selection(className, prefix, testNames))
          }
          else 
            None
        }
        else if (name == "should") {
          invocation.parent match {
            case invocationParent @ MethodInvocation(className, target, parent, children, "in", args) => 
              val testName = getTestName(prefix, invocationParent)
              Some(new Selection(className, testName, Array(testName)))
            case _ => 
              None
          }
        }
        else if (name == "in") {
          val testName = getTestName(prefix, invocation)
          Some(new Selection(className, testName, Array[String](testName)))
        }
        else 
          None
      case _ => None
    }
  }

  private def getTestNamesFromChildren(prefix: String, children: Array[AstNode]) = {
    children
      .filter(node => node.isInstanceOf[MethodInvocation] && isValidName(node.name, Set("in")))
      .map { node =>
        val invocation = node.asInstanceOf[MethodInvocation]
        getTestName(prefix, invocation)
      }
  }
  
  private def getTargetString(target: AstNode, prefix: String, postfix: String): String = {
    if (target == null)
      postfix
    else {
      target match {
        case MethodInvocation(className, targetTarget, parent, children, "should", args) if (args.length > 0) => 
          "should " + args(0).toString
        case _ => 
          target.toString
      }
    } 
  }
  
  private def getTestName(prefix: String, invocation: MethodInvocation) = {
    prefix + " " + getTargetString(invocation.target, prefix, "")
  }
}*/