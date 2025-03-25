import { error } from "console";

const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export async function apiFetch(endpoint: string, options: RequestInit = {}) {
    const token = localStorage.getItem("token"); // Récupère le JWT stocké

    const headers = {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}), // Ajoute le token si dispo
    };

    const response = await fetch(`${API_URL}/${endpoint}`, {
        ...options,
        headers: {
            ...headers,
            ...options.headers,
        },
    });

    if (!response.ok) {
        console.log(response)
        throw new Error(`API error: ${response.statusText}`);
    }

    return response.json();
}
