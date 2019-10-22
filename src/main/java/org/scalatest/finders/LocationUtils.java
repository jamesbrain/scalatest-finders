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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;


public class LocationUtils {
  
  private static List<Finder> getFinderInstances(Class<?> clazz) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, InstantiationException {
    Annotation[] annotations = clazz.getAnnotations();
    Annotation findersAnnotation = null;
    for (Annotation annotation : annotations) {
      if (annotation.annotationType().getName().equals("org.scalatest.Finders")) {
        findersAnnotation = annotation;
        break;
      }
    }
    List<Finder> finderList = new ArrayList<>();
    if (findersAnnotation != null) {
      Method valueMethod = findersAnnotation.annotationType().getMethod("value");
      String[] finderClassNames = (String[]) valueMethod.invoke(findersAnnotation);
      for (String finderClassName : finderClassNames) {
        Class<?> finderClass = clazz.getClassLoader().loadClass(finderClassName);
        Object instance = finderClass.newInstance();
        if (instance instanceof Finder)
          finderList.add((Finder) instance);
      }
    }
    
    return finderList;
  }
  
  private static List<Finder> lookInSuperClasses(Class<?> clazz) throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, InstantiationException {
    Class<?> superClass;
    List<Finder> finders = new ArrayList<>();
    while(finders.size() == 0 && (superClass = clazz.getSuperclass()) != null) {
      finders = getFinderInstances(superClass);
      clazz = superClass;
    }
    return finders;
  }
  
  private static List<Finder> lookInInterfaces(Class<?>[] interfaces) throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, InstantiationException {
    List<Finder> finders = new ArrayList<>();
    while(finders.size() == 0 && interfaces.length != 0) {
      List<Class<?>> newInterfaces = new ArrayList<>();
      for (Class<?> itf : interfaces) {
        finders = getFinderInstances(itf);
        if (finders.size() == 0)
          newInterfaces.addAll(Arrays.asList(itf.getInterfaces()));
        else
          break;
      }
      interfaces = newInterfaces.toArray(new Class<?>[0]);
    }
    return finders;
  }
  
  public static List<Finder> getFinders(Class<?> clazz) throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, InstantiationException {
    List<Finder> finders = getFinderInstances(clazz);
    if (finders.size() == 0) // Look for super interface first since style traits are compiled as Java interfaces
      finders = lookInInterfaces(clazz.getInterfaces());
    if (finders.size() == 0) // Look in super classes, in case custom test style is a class instead of trait.
      finders = lookInSuperClasses(clazz);
    return finders;
  }
  
  public static <T extends AstNode> T getParentOfType(AstNode node, Class<T> clazz) {
    T result = null;
    while (result == null && node.parent() != null) {
      if (clazz.isAssignableFrom(node.parent().getClass()))
        result = (T) node.parent();
      else
        node = node.parent();
    }
    return result; 
  }
  
  public static <T extends AstNode> AstNode getParentBeforeType(AstNode node, Class<T> clazz) {
    AstNode result = null;
    while (result == null && node.parent() != null) {
      if (clazz.isAssignableFrom(node.parent().getClass()))
        result = node;
      else
        node = node.parent();
    }
    return result;
  }
  
  public static boolean isValidName(String name, Set<String> validNames) {
    return validNames.contains(name);
  }
  
  public static boolean isSingleStringParamInvocationWithName(MethodInvocation invocation, Set<String> validNames) {
    return isValidName(invocation.name(), validNames) && invocation.args().length == 1;
  }
}

/*package org.scalatest.finders
import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer

object LocationUtils {
  
  private def getFinderInstance(clazz: Class[_]): Option[Finder] = {
    val styleOpt = clazz.getAnnotations.find(annt => annt.annotationType.getName == "org.scalatest.Style")
    styleOpt match {
      case Some(style) => 
        val valueMethod = style.annotationType.getMethod("value")
        val finderClassName = valueMethod.invoke(style).asInstanceOf[String]
        if (finderClassName != null) {
          val finderClass = clazz.getClassLoader.loadClass(finderClassName)
          val instance = finderClass.newInstance
          instance match {
            case finder: Finder => Some(finder)
            case _ => None
          }
        }
        else
          None
      case None => None
    }
    
  }
  
  @tailrec
  private def lookInSuperClasses(clazz: Class[_]): Option[Finder] = {
    val superClass = clazz.getSuperclass
    if (superClass != null) {
      val finder = getFinderInstance(superClass)
      finder match {
        case Some(finder) => Some(finder)
        case None => lookInSuperClasses(superClass)
      }
    }
    else
      None
  }
  
  @tailrec
  private def lookInInterfaces(interfaces: Array[Class[_]]): Option[Finder] = {
    if (interfaces.length > 0) {
      val interfaceWithFinderOpt = interfaces.find { getFinderInstance(_).isInstanceOf[Some[Finder]] }
      interfaceWithFinderOpt match {
        case Some(interfaceWithFinder) => getFinderInstance(interfaceWithFinder)
        case None => 
          val superInterfaces = new ListBuffer[Class[_]]
          superInterfaces ++= (interfaces.map { intf => intf.getInterfaces }.toList.flatten )
          lookInInterfaces(superInterfaces.toArray)
      }
    }
    else
      None
  }

  def getFinder(clazz: Class[_]) = {
    val ownFinderOpt = getFinderInstance(clazz)
    ownFinderOpt match {
      case Some(ownFinder) => Some(ownFinder)
      case None =>
        // Look for super interface first since style traits are compiled as Java interfaces
        val intfFinderOpt = lookInInterfaces(clazz.getInterfaces)
        intfFinderOpt match {
          case Some(intfFinder) => Some(intfFinder)
          case None => 
            // Look in super classes, in case custom test style is a class instead of trait.
            lookInSuperClasses(clazz)
        }
    }
  }
  
  @tailrec
  def getParentOfType[T <: AstNode](node: AstNode, clazz: Class[T]): Option[T] = {
    if (node.parent == null)
      None
    else {
      if (clazz.isAssignableFrom(node.parent.getClass))
        Some(node.parent.asInstanceOf[T])
      else
        getParentOfType(node.parent, clazz)
    }
  }
  
  @tailrec
  def getParentBeforeType[T <: AstNode](node: AstNode, clazz: Class[T]): Option[AstNode] = {
    if (node.parent == null)
      None
    else {
      if (clazz.isAssignableFrom(node.parent.getClass))
        Some(node)
      else
        getParentBeforeType(node.parent, clazz)
    }
  }
  
  def isSingleStringParamInvocationWithName(invocation: MethodInvocation, validNames: Set[String]): Boolean = {
    isValidName(invocation.name, validNames) && invocation.args.length == 1
  }
  
  def isValidName(name: String, validNames: Set[String]) = validNames.contains(name)
}*/