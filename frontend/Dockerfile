FROM nginx:alpine
# On supprime le contenu par défaut
RUN rm -rf /usr/share/nginx/html/*
# On copie TOUT le contenu de ton dossier local frontend
COPY . /usr/share/nginx/html/
EXPOSE 80