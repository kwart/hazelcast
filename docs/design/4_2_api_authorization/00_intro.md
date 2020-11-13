# API calls authorization

**Targeting Hazelcast Enterprise**

Internal PRD link: https://hazelcast.atlassian.net/wiki/spaces/PM/pages/2792653059

## Summary

Provide fine grained authorization capabilities on a API (e.g. `map.get` or `map.keySet`) calls level.

## Goals

* extend the current `Permission`-based authorization approach - add method level actions

## Non-Goals


## Motivation

The current permission model is focused on operation types (e.g. create/remove data structure, read/write data), but it
doesn't take into account other aspects as are the different method time complexity, influence on cluster stability, etc.
Use-cases similar to Cache-as-a-Service would benefit from ability do forbid some of the methods. There is no simple way
to resolve it with the current permission form.

## Description

* Replace bitset-based actions by methodname- and groupname-based ones in Hazelcast permission classes;
* Introduce mapping for "implies" (e.g. `map.put` permission implies both `map.put()` and `map.set()`)


**Open questions**

* Should be the overloaded methods covered by one permission? Or do we need extra permission for each form? (req 1)
* Do we want to introduce deny rules? How would the config look like then? (req 3, 4)

**Input for estimations**

## Alternatives


## Testing


## Risk and Assumptions

Finer grained permission support may result in significantly more permissions in the member configuration.
More permission definitions in the member configuration will cause their less performant evaluation.
