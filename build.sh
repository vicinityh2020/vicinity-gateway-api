# Build project and generate checksum
mvn clean package
cd target/ && shasum -a 256 ogwapi-jar-with-dependencies.jar > checksum.md5
