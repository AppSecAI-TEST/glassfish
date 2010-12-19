/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.flashlight.datatree.impl;

import com.sun.enterprise.util.ObjectAnalyzer;
import static com.sun.enterprise.util.SystemPropertyConstants.MONDOT;
import static com.sun.enterprise.util.SystemPropertyConstants.SLASH;
import org.glassfish.flashlight.datatree.TreeNode;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Harpreet Singh
 * @author Byron Nevins
 * 12/18/2010 -- Added encode/decode.  Note that the encoded form for a dot is
 * NOT something like "\\." -- there is too much code around making assumptions
 * about dots, splitting strings, etc.  So we replace with ___MONDOT___
 */
public abstract class AbstractTreeNode implements TreeNode, Comparable {

    protected Map<String, TreeNode> children =
            new ConcurrentHashMap<String, TreeNode>();
    protected String name;    // The node object itself
    protected String category;
    protected String description;
    protected boolean enabled = false;
    private static String NAME_SEPARATOR = ".";
    private static String REGEX = "(?<!\\\\)\\.";
    private TreeNode parent = null;
    // Special character Regex to be converted to .* for v2 compatibility
    private String STAR = "*";

    @Override
    public String toString() {
        return ObjectAnalyzer.toString(this);
    }

    @Override
    public String getName() {

        return decodeName();
    }

    @Override
    public void setName(String aname) {

        if (aname == null)
            throw new RuntimeException("Flashlight-utils: Tree Node needs a"
                    + " non-null name");
        name = encodeNodeName(aname);
    }

    // should be implemented at the sub-class level
    @Override
    public Object getValue() {
        if (enabled) {
            return getChildNodes();
        }
        return null;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public TreeNode addChild(TreeNode newChild) {
        if (newChild == null) {
            return null;
        }
        else if (newChild.getName() == null) {
            // log it and return null
            return null;
        }
        newChild.setParent(this);
        return children.put(newChild.getName(), newChild);
    }

    @Override
    public String getCompletePathName() {

        if (getParent() != null) {
            return getParent().getCompletePathName()
                    + this.NAME_SEPARATOR + getName();
        }
        else {
            return getName();
        }
    }

    @Override
    public void setParent(TreeNode parent) {
        this.parent = parent;
    }

    @Override
    public TreeNode getParent() {
        return this.parent;
    }

    /**
     * Returns a mutable view of the children
     * @return
     */
    @Override
    public Collection<TreeNode> getChildNodes() {
        return children.values();
    }

    /**
     * Returns a mutable view of the children
     * @return
     */
    @Override
    public Collection<TreeNode> getEnabledChildNodes() {
        List<TreeNode> childNodes = new ArrayList();
        for (TreeNode child : children.values()) {
            if (child.isEnabled())
                childNodes.add(child);
        }
        return childNodes;
    }

    public Enumeration<TreeNode> getChildNodesImmutable() {

        return ((ConcurrentHashMap) children).elements();
    }

    @Override
    public boolean hasChildNodes() {
        return !children.isEmpty();

    }

    @Override
    public void removeChild(TreeNode oldChild) {
        String child = oldChild.getName();
        if (child != null) {
            children.remove(child);
        }
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public TreeNode getChild(String childName) {
        if (childName == null) {
            return null;
        }
        else {
            return children.get(childName);
        }
    }

    @Override
    public TreeNode getNode(String completeName) {
        if (completeName == null) {
            return null;
        }
        completeName = encodePath(completeName);
        Pattern pattern = Pattern.compile(AbstractTreeNode.REGEX);
        String[] tokens = pattern.split(completeName);
        TreeNode n = findNodeInTree(tokens);
        return n;
    }

    // confused?  That's expected!  This should be refactored/re-done for 3.2
    // we store dots and slashes encoded.  THe tokens coming in to this method
    // are encoded.  That's because there is lots of other code scattered around
    // that looks for these special meta-characters.  To be safe I'm storing them
    // in the node encoded.
    // But the "children" object has keys that come from the getName() of the node
    // which is the value.
    private TreeNode findNodeInTree(String[] tokens) {
        if (tokens == null) {
            return null;
        }
        TreeNode child = getChild(tokens[0]);

        if (child == null)
            child = getChild(decodeName(tokens[0]));

        if (child == null)
            return null;

        if (tokens.length > 1)
            child = ((AbstractTreeNode) child).findNodeInTree(dropFirstStringToken(tokens));

        return child;

    }

    private String[] dropFirstStringToken(String[] token) {
        if (token.length == 0) {
            return null;
        }
        if (token.length == 1) {
            return null;
        }
        String[] newToken = new String[token.length - 1];
        for (int i = 0; i < newToken.length; i++) {
            newToken[i] = token[i + 1];
        }
        return newToken;
    }

    /**
     * Returns all the nodes under the current tree
     * @return List of all nodes in the current tree
     */
    @Override
    public List<TreeNode> traverse(boolean ignoreDisabled) {
//        System.out.println ("Node: " + this.getName ()+ " is enabled "+isEnabled());
        List<TreeNode> list = new ArrayList<TreeNode>();

        if (ignoreDisabled) {
            if (!this.enabled) {
                return list;
            }
        }
        list.add(this);

        if (!hasChildNodes()) {
            return list;
        }

        Collection<TreeNode> childList = children.values();
        for (TreeNode node : childList) {
            list.addAll(node.traverse(ignoreDisabled));
        }
        return list;
    }

    @Override
    public List<TreeNode> getNodes(String pattern, boolean ignoreDisabled, boolean gfv2Compatible) {
        pattern = pattern.replace("\\.", "\\\\\\.");

        // bnevins October 2010
        // design gotcha -- It used to be IMPOSSIBLE to tell the difference between
        // a literal slash in a name and a slash used as a delimiter.  Deep down
        // under dozens of calls in the stack -- Strings are concatanated together
        // Simple solution is to replace literal slashes with a token.  The probe
        // provider code needs to do that.  jndi names are an example of this.
        // Here we replace slash in the given pattern with the token to pull out
        // the right stuff.
        // This is a ARCHITECTURE flaw.  This hack can be replaced with an
        // ARCHITECTURAL fix later if desired.
        pattern = pattern.replace("/", SLASH);

        List<TreeNode> regexMatchedTree = new ArrayList<TreeNode>();


        try {
            if (gfv2Compatible)
                pattern = convertGFv2PatternToRegex(pattern);

            Pattern mPattern = Pattern.compile(pattern);
            List<TreeNode> completeTree = traverse(ignoreDisabled);

            for (TreeNode node : completeTree) {
                Matcher matcher = mPattern.matcher(node.getCompletePathName());

                if (matcher.matches()) {
                    regexMatchedTree.add(node);
                }
            }
        }
        catch (java.util.regex.PatternSyntaxException e) {
            // log this
            // e.printStackTrace ();
        }
        return regexMatchedTree;
    }

    @Override
    public List<TreeNode> getNodes(String pattern) {
        return getNodes(pattern, true, true);
    }

    private String convertGFv2PatternToRegex(String pattern) {
        if (pattern.equals(STAR)) {
            return ".*";
        }
        // Doing this intermediate step as replacing "*" in a pattern with ".*"
        // is too hassling

        String modifiedPattern = pattern.replaceAll("\\*", ":");
        String regex = modifiedPattern.replaceAll(":", ".*");
        return regex;
    }

    @Override
    public int compareTo(Object o) {
        return this.getName().compareTo(((TreeNode) o).getName());
    }

    @Override
    public TreeNode getPossibleParentNode(String pattern) {
        // simplify by bailing out early if preconditions are not met...
        if (pattern == null || pattern.length() <= 0 || pattern.indexOf('*') >= 0)
            return null;

        TreeNode node = null;
        int longest = 0;

        for (TreeNode n : traverse(true)) {
            String aname = n.getCompletePathName();

            if (aname == null)
                continue;   // defensive pgming

            if (pattern.startsWith(aname)) {
                int thisLength = aname.length();

                // keep the longest match ONLY!
                if (node == null || thisLength > longest) {
                    node = n;
                    longest = thisLength;
                }
            }
        }

        return node;
    }

    private String encodeNodeName(String nodeName) {
        return nodeName.replace(".", MONDOT).replace("/", SLASH).replace("\\/", SLASH).replace("\\.", MONDOT);
    }

    private String encodePath(String thePath) {
        // REST encodes (1) to (2)
        //  aaa    bbb.x   cccc
        //  aaa.bbb\\x.cccc
        // we want aaa.bbb___MONDOT___x.cccc

        return thePath.replace("\\/", SLASH).replace("\\.", MONDOT);
    }

    // todo replace with \\. ???
    private String decodeName() {
        return decodeName(name);
    }

    private static String decodeName(String s) {
        return s.replace(SLASH, "/").replace(MONDOT, ".");
    }
}
