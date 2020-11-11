# Support for hostnames in member configuration

Issues:
* https://github.com/hazelcast/hazelcast/issues/16651
* https://github.com/hazelcast/hazelcast/issues/12334
* https://github.com/hazelcast/hazelcast/issues/15722
* https://github.com/hazelcast/hazelcast-enterprise/issues/3627

Internal PRD link: https://hazelcast.atlassian.net/wiki/spaces/PM/pages/2347827205

## Summary

Allow a seamless usage of hostnames in the member configuration (e.g. cluster member list, WAN configuration).

## Goals

* fix issues linked in the header
* support hostname-based addressing in member-to-member communication
* support hostname-based addressing in WAN configuration
* verify the hostname-based addressing works in relevant integration plugins (e.g. discovery in AWS, Azure, GCP, Kubernetes)

## Non-Goals

* support cases where the hostname resolves to multiple IP addresses

## Motivation

Hazelcast as a distributed system depends on networking. It fully supports member addressing through IP addresses.
It's not sufficient for some usecases where hostnames plays an important role.

## Description

The initial step towards the resolution was taken in [PR #16326](https://github.com/hazelcast/hazelcast/pull/16326)
(Hazelcast 4.0). It introduced unique member identifier (UUID) into the `BindMessage`. The code became part of the
`MemberHandshake` and `ServerContext` classes during code reorganization in Hazelcast 4.1 (see 
[PR #16814](https://github.com/hazelcast/hazelcast/pull/16814), [PR #16889](https://github.com/hazelcast/hazelcast/pull/16889)).

The UUID will be used as a replacement for the `Address` instances when referencing other members
(e.g. in map of existing member-to-member connections).

As we don't know UUIDs of other members upfront, connections will need to be established purely on the address basis
and they will be moved to the UUID-based map once confirmed.

Mapping mechanisms will be introduced:
- UUID-to-Addresses;
- Address-to-UUID.

**Open questions**

* What if the hostname resolves to different IP addresses on different members?
* What should take precedence - IP or hostname? The TLS favorites using hostnames.
* How should the member-list look like?

**Input for estimations**
There is cca 420 (+73 enterprise) Java classes importing the `com.hazelcast.cluster.Address` in Hazelcast 4.1.
Out of it, cca 255 (+35) are artifact ones and the rest is in the testsuite.

These places need to be checked if they require changes related to new mappings.

## Alternatives

Support IP-addressing only: It's the current approach which prevents using Hazelcast in some scenarios. 

## Testing

* Verify that hostnames in TCP-IP member configuration works;
* verify that hostnames in WAN configuration works;
* verify that a hostname available only after the member starts works when used for establishing connections from other members;
* verify that it's possible to use multiple hostnames to reference one member;
* *(Enterprise)* verify Hot Restart remains working;
* *(Enterprise)* verify TLS remains working (when host validation is enabled);
* verify the performance doesn't significantly drop.

## Risk and Assumptions

