#!/bin/bash
set -e
docker build -t registry.sonata-nfv.eu:5000/son-sp-infrabstract vim-adaptor/Dockerfile .
