"use client";

import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";

export default function Dashboard() {
  const searchParams = useSearchParams();
  const token = searchParams.get("token");
  const [user, setUser] = useState(null);

  useEffect(() => {
    if (token) {
      localStorage.setItem("jwt", token);
    }
  }, [token]);

  useEffect(() => {
    const fetchUserData = async () => {
      const storedToken = localStorage.getItem("jwt");
      if (!storedToken) return;

      try {
        const response = await fetch("http://localhost:8080/api/auth/spotify/me", {
          headers: {
            Authorization: `Bearer ${storedToken}`,
          },
        });

        if (!response.ok) throw new Error("Erreur lors de la récupération des données");

        const data = await response.json();
        setUser(data);
      } catch (error) {
        console.error("Erreur :", error);
      }
    };

    fetchUserData();
  }, []);

  return (
    <div>
      <h1>Bienvenue sur le Dashboard</h1>
      {user ? (
        <div>
          <p><strong>Nom :</strong> {user.display_name}</p>
          <img src={user.images?.[0]?.url} alt="Avatar" width={100} />
        </div>
      ) : (
        <p>Chargement des données...</p>
      )}
    </div>
  );
}
