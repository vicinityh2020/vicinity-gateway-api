# Update version
mvn versions:set -DnewVersion=<SOME_VERSION>

# Run tests
mvn  [ -Dtest=TestApp1,TestApp2 ] test
