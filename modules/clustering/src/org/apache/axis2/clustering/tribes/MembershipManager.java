/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.axis2.clustering.tribes;

import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.membership.MemberImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Responsible for managing the membership. Handles membership changes.
 */
public class MembershipManager {
    /**
     * List of members in the cluster
     */
    private final List<Member> members = new ArrayList<Member>();

    /**
     * The member representing this node
     */
    private Member localMember;

    public Member getLocalMember() {
        return localMember;
    }

    public void setLocalMember(Member localMember) {
        this.localMember = localMember;
    }

    /**
     * A new member is added
     *
     * @param member The new member that joined the cluster
     */
    public synchronized void memberAdded(Member member) {
        if (!members.contains(member)) {
            members.add(member);
        }
    }

    /**
     * A member disappeared
     *
     * @param member The member that left the cluster
     */
    public synchronized void memberDisappeared(Member member) {
        members.remove(member);
    }

    /**
     * Get the list of current members
     *
     * @return list of current members
     */
    public synchronized Member[] getMembers() {
        return members.toArray(new Member[members.size()]);
    }

    /**
     * Get the member that has been alive for the longest time
     *
     * @return The member that has been alive for the longest time
     */
    public synchronized Member getLongestLivingMember() {
        Member longestLivingMember = null;
        if (members.size() > 0) {
            Member member0 = members.get(0);
            long longestAliveTime = member0.getMemberAliveTime();
            longestLivingMember = member0;
            for (Member member : members) {
                if (longestAliveTime < member.getMemberAliveTime()) {
                    longestAliveTime = member.getMemberAliveTime();
                    longestLivingMember = member;
                }
            }
        }
        return longestLivingMember;
    }

    /**
     * Get a random member from the list of current members
     *
     * @return A random member from the list of current members
     */
    public synchronized Member getRandomMember() {
        if (members.size() == 0) {
            return null;
        }
        int memberIndex = new Random().nextInt(members.size());
        return members.get(memberIndex);
    }

    /**
     * Check whether there are any members
     *
     * @return true if there are other members, false otherwise
     */
    public boolean hasMembers() {
        return members.size() > 0;
    }

    /**
     * Get a member
     *
     * @param member The member to be found
     * @return The member, if it is found
     */
    public Member getMember(Member member) {
        if (hasMembers()) {
            MemberImpl result = null;
            for (int i = 0; i < this.members.size() && result == null; i++) {
                if (members.get(i).equals(member)) {
                    result = (MemberImpl) members.get(i);
                }
            }
            return result;
        }
        return null;
    }
}