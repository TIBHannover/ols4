# TIB Terminology Service Backend

This project is forked from EBI's OLS4 repository and it is used as the backend of the TIB Terminology Service that can be accessed from https://api.terminology.tib.eu/swagger-ui/index.html. 
While it is installed as a full product with EBI's branding, the frontend module is not used by TIB and another TIB maintained repository at https://github.com/TIBHannover/TIB-Terminology-Service-Frontend-2.0 is used for this purpose. 
Furthermore, TIB maintains only the docker based and local installation pipelines from this repository along with an ansible based installation pipeline that is specific to TIB. 
The Kubernetes based installation pipeline of EBI is not used and maintained by TIB although its configuration files are preserved in the repository. 
The instructions for these different installation are listed in the links below:
* If you want to perform automated installation using ansible and vagrant, check out https://github.com/TIBHannover/ols4-box
* If you want to install by docker or locally, the base documentation of EBI can be accessed from here: [README_EBI.md](./README_EBI.md)

