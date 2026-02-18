export type Project = {
  id: number;
  githubRepoId: string | null;
  name: string;
  description: string | null;
  url: string | null;
  languages: string[];
  stars: number;
  forks: number;
  highlighted: boolean;
  displayOrder: number;
};

export type Skill = {
  id: number;
  name: string;
  category: string | null;
  proficiency: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED' | 'EXPERT';
  displayOrder: number;
};
