# Autenticación (Lab #5, tutorial 1): valores del "Configuration file preview" de la API key
variable "tenancy_ocid" {
  type = string
}

variable "user_ocid" {
  type = string
}

variable "fingerprint" {
  type = string
}

variable "private_key_path" {
  description = "Ruta absoluta a la llave privada de la API (fuera del repo y de OneDrive)"
  type        = string
}

variable "region" {
  type    = string
  default = "us-ashburn-1"
}

# Recursos existentes que se reutilizan
variable "compartment_ocid" {
  description = "Compartimento IF6000-26-GUI"
  type        = string
}

variable "subnet_ocid" {
  description = "Subred pública 10.0.8.0/24 de la VCN if6000"
  type        = string
}

variable "ad_index" {
  description = "Índice del AD (0, 1 o 2). Debe ser el mismo de la VM del frontend: E2.1.Micro solo es Always Free en un AD"
  type        = number
}

# VM del backend
variable "ssh_public_key_path" {
  type    = string
  default = "ssh-keys/democraxssy-back.pub"
}

variable "api_domain" {
  description = "Subdominio de la API, por ejemplo api-democraxssy-if6000.duckdns.org"
  type        = string
}

variable "jvm_max_heap" {
  description = "Tope de memoria de la JVM (la VM tiene 1 GB)"
  type        = string
  default     = "256m"
}
