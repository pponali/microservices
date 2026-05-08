{{/* Full image ref for a given service name */}}
{{- define "ms.image" -}}
{{- $g := .Values.global -}}
{{ $g.image.registry }}/{{ $g.image.repository }}/{{ .name }}:{{ $g.image.tag }}
{{- end -}}

{{/* Common labels */}}
{{- define "ms.labels" -}}
app.kubernetes.io/name: {{ .name }}
app.kubernetes.io/instance: {{ .root.Release.Name }}
app.kubernetes.io/managed-by: helm
tier: {{ .tier | default "app" }}
{{- end -}}
