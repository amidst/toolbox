.. AMIDST documentation master file, created by
   sphinx-quickstart on MON Nov  5 10:43:21 2018.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

.. image:: _static/img/logo.png
   	:scale: 50 %
   	:align: center

|

About AMIDST
============

What is AMIDST?
---------------

AMIDST is an open source Java toolbox for scalable probabilistic machine
learning with a special focus on (massive) streaming data. The toolbox
allows specifying probabilistic graphical models with latent variables
and temporal dependencies.



The main features of the tooblox are listed below:

-  **Probabilistic Graphical Models**: Specify your model using
   probabilistic graphical models with latent variables and temporal
   dependencies. AMIDST contains a large list of predefined latent
   variable models:

-  **Scalable inference**: Perform inference on your probabilistic
   models with powerful approximate and scalable algorithms.

-  **Data Streams**: Update your models when new data is available. This
   makes our toolbox appropriate for learning from (massive) data
   streams.

-  **Large-scale Data**: Use your defined models to process massive data
   sets in a distributed computer cluster using Apache Flink or (soon)
   Apache Spark.

-  **Extensible**: Code your models or algorithms within AMiDST and
   expand the toolbox functionalities. Flexible toolbox for researchers
   performing their experimentation in machine learning.

-  **Interoperability**: Leverage existing functionalities and
   algorithms by interfacing to other software tools such as Hugin, MOA,
   Weka, R, etc.


.. toctree::
   :includehidden:
   :maxdepth: 1
   :caption: General Information
   
   notes/about-amidst

.. toctree::
   :includehidden:
   :maxdepth: 1
   :caption: Examples
   
   notes/sparklink
   notes/wekalink
   notes/latent-variable-models
   notes/flinklink
   notes/dbnetworks
   notes/bnetworks


.. toctree::
   :includehidden:
   :maxdepth: 1
   :caption: First steps
   
   notes/getting-started
   notes/requirements
   notes/loading-dependencies
   notes/installing-local-repository
   notes/generating-packages
  
.. toctree::
   :includehidden:
   :maxdepth: 1
   :caption: Contributing to AMIDST
   
   notes/basic-steps-contributing

.. toctree::
   :includehidden:
   :maxdepth: 1
   :caption: Other

   JavaDoc <http://www.amidsttoolbox.com/documentation/0-7-2/other-072/javadoc/>


.. Indices and tables
  ==================

  * :ref:`genindex`
  * :ref:`modindex`
  * :ref:`search`
  



.. role:: bash(code)
   :language: bash
.. role:: python(code)
   :language: python
