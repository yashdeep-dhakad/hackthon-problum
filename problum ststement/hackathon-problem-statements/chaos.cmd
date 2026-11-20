@echo off
rem Runs the settlement chaos run (about 5 minutes). Results: chaos\target\surefire-reports\
call mvnw.cmd -B -pl chaos -am verify -DskipChaos=false -Dtest=SettlementChaosRun -Dsurefire.failIfNoSpecifiedTests=false %*
