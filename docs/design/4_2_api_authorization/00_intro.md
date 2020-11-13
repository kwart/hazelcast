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


**Current form of permission definition**
```xml
<map-permission name="custom" principal="dev">
    <endpoints>
        <endpoint>127.0.0.1</endpoint>
    </endpoints>
    <actions>
        <action>create</action>
        <action>put</action>
    </actions>
</map-permission>
```

**Simple authorization extension on method level**
```xml
<map-permission name="custom" principal="dev">
    <endpoints>
        <endpoint>127.0.0.1</endpoint>
    </endpoints>
    <actions>
        <action>create</action>
        <action>set()</action>
        <action>put()</action>
    </actions>
</map-permission>
```

**Authorization extension with deny rules**
*(if we decide for the deny rules)*
This introduces `root-policy` configuration on `client-permissions` config with values `allow` and `deny` (default).
Each permission would have then the optional `deny` attribute (defaulting to `false` for backward compatibility).
```xml
<client-permissions on-join-operation="RECEIVE" root-policy="allow">
    <map-permission name="default" deny="true">
        <actions>
            <action>values()</action>
            <action>keySet()</action>
        </actions>
    </map-permission>
</client-permissions>
```

**Open questions**

* Should be the overloaded methods covered by one permission? Or do we need extra permission for each form? (req 1)
* Do we want to introduce deny rules? How would the config look like then? (req 3, 4)

**Input for estimations**

## Alternatives

The root rule (allow/deny) configuration could be moved as an option to the `client-permission-policy` param 
```xml
<client-permission-policy class-name="com.hazelcast.security.impl.DefaultPermissionPolicy">
    <properties>
        <property name="root-rule">allow</property>
    </properties>
</client-permission-policy>
```
The disadvantage is this wouldn't be covered by the typed-configuration. So it would be more error prone.

## Testing


## Risk and Assumptions

Finer grained permission support may result in significantly more permissions in the member configuration.
More permission definitions in the member configuration will cause their less performant evaluation.
