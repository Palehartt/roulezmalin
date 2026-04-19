import json

# Configuration des noms de fichiers
input_file = 'europcar_agencies.json'
output_file = 'europcar_agencies_nettoyee.json'

def filter_json():
    try:
        # 1. Chargement des données initiales
        with open(input_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        # Si le JSON est une liste d'objets
        filtered_data = []

        for item in data:
            # 2. Extraction ciblée des données
            # On utilise .get() pour éviter les erreurs si une clé est manquante
            clean_item = {
                "id": item.get("id"),
                "Nom": item.get("information", {}).get("name"),
                "latitude": item.get("geoPosition", {}).get("lat"),
                "longitude": item.get("geoPosition", {}).get("lng")
            }
            filtered_data.append(clean_item)

        # 3. Sauvegarde du nouveau fichier
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(filtered_data, f, indent=4, ensure_ascii=False)
            
        print(f"Succès ! {len(filtered_data)} objets ont été traités et sauvegardés dans {output_file}")

    except FileNotFoundError:
        print(f"Erreur : Le fichier {input_file} est introuvable.")
    except json.JSONDecodeError:
        print("Erreur : Le fichier source n'est pas un JSON valide.")

if __name__ == "__main__":
    filter_json()
