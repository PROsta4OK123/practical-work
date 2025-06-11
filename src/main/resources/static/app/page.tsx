import { Navbar } from "@/components/navbar"
import { HomeContent } from "@/components/home-content"

export default function Home() {
  return (
    <div className="min-h-screen flex flex-col">
      <Navbar />
      <HomeContent />
    </div>
  )
}
