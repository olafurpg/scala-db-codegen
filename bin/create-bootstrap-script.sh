 coursier bootstrap \
    -r https://oss.sonatype.org/content/groups/public \ # optional if already synced to maven
    com.geirsson:scala-db-codegen_2.11:0.2.2 \
    -f -o scala-db-codegen \
    -M com.geirsson.codegen.Codegen
