# Nerdrolebot
This is the NerdBot for Xemor's Server.

Currently, it ranks all people by the amount of messages they sent in the past 2 weeks and takes the top 50%
of those who have sent at least one message and gives these people Nerd role. If at any time they drop below
the threshold, it is removed.

# Contributing

Feel free to PR if you have something to commit.  
The pom.xml includes a dependency for a 'snapshot' version of sqlite-jdbc which needs to be locally installed if you are planning to compile to make a release.  
This is done via the [Maven Install plugin](https://maven.apache.org/guides/mini/guide-3rd-party-jars-local.html).  
The jar has a pom.xml, and so only -Dfile needs to be satisfied.  
Otherwise, you can use the [regular jdbc](https://mvnrepository.com/artifact/org.xerial/sqlite-jdbc).  
Javadoc available [here:](https://drive.google.com/file/d/1kcNNeCHLhNZMqZ8qj0Pgn3oPCM-A4Ux9/view?usp=sharing)   
You need only download and extract the zip file, and open index.html.
