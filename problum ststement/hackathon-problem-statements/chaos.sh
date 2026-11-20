#!/usr/bin/env bash
# Runs the settlement chaos run (about 5 minutes). Results: chaos/target/surefire-reports/
exec ./mvnw -B -pl chaos -am verify -DskipChaos=false -Dtest='SettlementChaosRun' -Dsurefire.failIfNoSpecifiedTests=false "$@"
