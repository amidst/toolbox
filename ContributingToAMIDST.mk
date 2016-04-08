# Contributing to AMIDST <a name="extension"></a>

Developers are expected to contribute to this open software following the [Fork & Pull](https://help.github.com/articles/using-pull-requests/) collaboration model. Read this [guide](https://guides.github.com/activities/forking/) for full details about how to fork a project and make a pull request.

We establish the following categorization for the contributions to the toolbox. Each one is associated with a different collaboration schemes which is also detailed below.  

## (A) Basic Contributions

They encompass those contributions to the code that do not imply any major change or addition. For example, fixing a bug, adding methods to existing classes, adding new utility classes, etc. This contributions are made through a [pull request](https://help.github.com/articles/using-pull-requests/), which will be examined by the core group of developers of the project. 

## (B) Major Extensions

They refer to those contributions which aim to include a new functionality in the toolbox. For example, new inference methods, new learning algorithms, new concept-drift detection methods, new PGMs, new links to other toolboxes, etc. These extensions or new functionalities will be integrated as new Maven modules and will be located in the folder *[project-root-folder]/extensions/*. Then, contributing with a new extension will be based on the following three steps: (i) create a new Maven module using IntelliJ (follow this [link](https://www.jetbrains.com/idea/help/creating-maven-module.html) for details); then (ii) code your new algorithm inside this module; and (iii) make a [pull request](https://help.github.com/articles/using-pull-requests/) to add the new functionality to the project repository. 

All the provided extensions should fulfill the following basic quality requirements to be accepted as extensions by the AMIDST core team. 
* They should contain a readme.txt file detailing the functionality and scope of the extension. It also needs to specify if it is supported by a companion paper, student project, etc.

* The code should be well documented following [JavaDoc](https://en.wikipedia.org/wiki/Javadoc) standards. 
 
* It has to include [JUnit](www.junit.org/) tests which verify the correctness of the results produced by the provided code. 

## (C) Use-Cases 

They refer to those contributions which do not add any specific functionality to the toolbox. They can be seen as examples of how this toolbox can be used. This category might include contributions related to student projects, research papers, industry applications, etc. The AMIDST core team will not supervise the quality of the contributions, this is responsibility of the contributors. They  will be integrated as independent Maven modules and will be placed in a different code repository on github, https://github.com/amidst/toolbox-use-cases/, where they are expected to be submitted using, again, a [pull request](https://help.github.com/articles/using-pull-requests/) approach. 
