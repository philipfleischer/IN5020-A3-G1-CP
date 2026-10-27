# IN5020-A3-G1-CP
This is the third assignment for Group 1 in the Course IN5020 Distributed Systems

# Kjør programmet

# kompiler
javac -d bin $(find src -name "*.java")

# kjør alle konfigurasjoner spesifisert i oppgaven
java -cp bin Simulator 10 10
java -cp bin Simulator 100 20
java -cp bin Simulator 1000 20

# output lagres i output folder
