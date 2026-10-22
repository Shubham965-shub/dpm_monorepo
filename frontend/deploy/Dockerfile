# PRD01 frontend image: build the SPA, serve it from nginx with an /api/ reverse proxy to the backend
# (single host - no CORS). VITE_* values are baked at build time; BACKEND_ORIGIN is a runtime app
# setting, so pointing at a different backend never needs a rebuild.
#
# Build context = frontend/. Built by deploy/prd01/deploy-frontend.sh via ACR Tasks:
#   az acr build -r <acr> -f deploy/Dockerfile --secret-build-arg NODE_AUTH_TOKEN=<pat> \
#     [--build-arg VITE_POSTHOG_KEY=... --build-arg VITE_POSTHOG_HOST=...] -t helios-frontend:<tag> .
FROM node:22-alpine AS build
WORKDIR /app
# Only the token precedes npm ci, so changing the VITE_* args below never invalidates the install layer.
ARG NODE_AUTH_TOKEN
COPY package.json package-lock.json ./
RUN printf '@strides-pharma-science-ltd:registry=https://npm.pkg.github.com\n//npm.pkg.github.com/:_authToken=${NODE_AUTH_TOKEN}\n' > .npmrc \
    && npm ci \
    && rm .npmrc
COPY . .
# Empty = same origin: every backend route already starts with /api/, which nginx proxies.
ARG VITE_API_URL=
ARG VITE_POSTHOG_KEY=
ARG VITE_POSTHOG_HOST=
# Unset hides the header's Feedback button (FeedbackLauncher.tsx).
ARG VITE_FEEDBACK_API_BASE_URL=
ENV VITE_API_URL=$VITE_API_URL \
    VITE_POSTHOG_KEY=$VITE_POSTHOG_KEY \
    VITE_POSTHOG_HOST=$VITE_POSTHOG_HOST \
    VITE_FEEDBACK_API_BASE_URL=$VITE_FEEDBACK_API_BASE_URL
RUN npm run build

FROM nginx:1.27-alpine
# nginx:alpine's entrypoint envsubsts /etc/nginx/templates/*.template with the container env.
COPY deploy/nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
