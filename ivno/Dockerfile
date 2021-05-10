FROM ivnocordabase:latest

ARG IVNO_VERSION

# copy all CorDapps from deployNodes directory
COPY ["ivno-collateral-token-deploynodes/build/nodes/Bank A/cordapps/*.jar", "${CORDAPPS_DIR}/"]


# for debugging
##COPY --chown=ivno:ivno keep-alive.sh keep-alive.sh
#COPY keep-alive.sh keep-alive.sh
#
#CMD ["./keep-alive.sh"]
CMD ["run-corda"]
