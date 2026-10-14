FROM elasticsearch:9.2.1
COPY analysis-ik-9.2.1.zip /tmp/analysis-ik-9.2.1.zip
RUN elasticsearch-plugin install --batch file:///tmp/analysis-ik-9.2.1.zip